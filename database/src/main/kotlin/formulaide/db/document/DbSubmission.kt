package formulaide.db.document

import formulaide.api.data.*
import formulaide.api.search.SearchCriterion
import formulaide.api.types.Ref
import formulaide.api.types.ReferenceId
import formulaide.db.Database
import formulaide.db.document.DbSubmissionData.Companion.rootId
import formulaide.db.document.DbSubmissionData.Companion.toApi
import formulaide.db.document.DbSubmissionData.Companion.toDbSubmissionData
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bson.conversions.Bson
import org.litote.kmongo.*
import java.util.regex.Pattern
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

//region Submission data

/**
 * In-database representation of a [FormSubmission]'s [data][FormSubmission.data].
 *
 * The main differences are that:
 * - This schema is recursive, unlike `data`
 * - This schema must always have a root node (named after [rootId])
 * - Multiple answers to the same field are not under a common parent, but are put side by side with the same [key], with [isList] set to `true`.
 *
 * These differences are made so it is easier to write Mongo query through the structure.
 *
 * @property isList If this response is given to a field with a max [arity][formulaide.api.fields.FormField.arity] greater than 1.
 */
@Serializable
data class DbSubmissionData(
	@SerialName("k") val key: String,
	@SerialName("v") val value: String? = null,
	@SerialName("c") val children: List<DbSubmissionData> = emptyList(),
	@SerialName("l") val isList: Boolean = false,
) {

	override fun toString() = "DbSub($key" +
			(if (value != null) ": $value" else "") +
			(if (isList) " (list)" else "") +
			(if (children.isNotEmpty()) ", $children" else "") +
			")"

	companion object {
		private const val rootId = "r"

		fun ParsedSubmission.toDbSubmissionData(): DbSubmissionData {
			fun convert(field: ParsedField<*>): List<DbSubmissionData> = when (field) {
				is ParsedSimple -> listOf(
					DbSubmissionData(field.key,
					                 value = field.rawValue)
				)
				is ParsedUnion<*, *> -> listOf(
					DbSubmissionData(field.key,
					                 value = field.value.id,
					                 children = field.children.flatMap { convert(it) })
				)
				is ParsedComposite<*> -> listOf(
					DbSubmissionData(field.key,
					                 children = field.children.flatMap { convert(it) })
				)
				is ParsedList<*> -> field.children.flatMap { convert(it) }
					.map { it.copy(key = field.key, isList = true) }
			}

			return DbSubmissionData(rootId, children = this.fields.flatMap(::convert))
		}

		fun DbSubmissionData.toApi(): Map<String, String> {
			require(key == rootId) { "La fonction DbSubmissionData.toApi est faite pour travailler sur la racine de la saisie, mais la racine ne s'appelle pas '$rootId' : $key" }

			println("\nConverting db submission data…\nFull: $this")
			val map = HashMap<String, String>()

			fun convert(data: DbSubmissionData, parentKey: List<String>) {
				val current = "${(parentKey + data.key).joinToString(separator = ":")}\t"
				println("$current                  $data")

				if (data.value != null) {
					println("$current Value:           ${data.value}")
					map[(parentKey + data.key).joinToString(separator = ":")] = data.value
				}

				if (data.children.isNotEmpty()) {
					for (child in data.children.filter { !it.isList }) {
						println("$current Child:           $child")
						convert(child, parentKey + data.key)
					}

					val listChildren = data.children.filter { it.isList }.groupBy { it.key }
					for ((key, children) in listChildren) {
						println("$current List children:   $children")

						val editedChildren = children
							.mapIndexed { i, it -> it.copy(key = i.toString(), isList = false) }
						println("$current Edited children: $editedChildren")

						val list = DbSubmissionData(key, children = editedChildren)
						convert(list, parentKey + data.key)
					}
				}
			}

			convert(this, emptyList())

			return map.mapKeys { (k, _) -> k.substringAfter("$rootId:") }
		}
	}
}

//endregion

@Serializable
data class DbSubmission(
	val apiId: String,
	val form: ReferenceId,
	val root: ReferenceId? = null,
	val data: DbSubmissionData,
)

suspend fun Database.saveSubmission(submission: FormSubmission): DbSubmission {
	val composites = listComposites()

	val form = findForm(submission.form.id)
		?: error("Une saisie a été reçue pour le formulaire '${submission.form}', qui n'existe pas.")

	form.load(composites)
	form.validate()
	val parsed = submission.parse(form)

	return DbSubmission(
		form = form.id,
		data = parsed.toDbSubmissionData(),
		root = submission.root?.id,
		apiId = newId<DbSubmission>().toString()
	).also {
		submissions.insertOne(it)
	}
}

suspend fun Database.findSubmission(form: ReferenceId): List<DbSubmission> =
	submissions.find(DbSubmission::form eq form).toList()

suspend fun Database.findSubmissionById(id: ReferenceId): DbSubmission? =
	submissions.findOne(DbSubmission::apiId eq id)

fun DbSubmission.toApi() = FormSubmission(apiId, Ref(form), root?.let { Ref(it) }, data.toApi())

suspend fun Database.searchSubmission(
	form: Form,
	root: Action?,
	criteria: List<SearchCriterion<*>>,
): List<DbSubmission> {
	val filter = ArrayList<Bson>()
	filter += DbSubmission::form eq form.id
	filter += DbSubmission::root eq root?.id

	for (criterion in criteria) {
		val ids = criterion.fieldKey.split(":")
		require(ids.isNotEmpty()) { "Un critère de recherche doit obligatoirement préciser la clef d'un champ, trouvé: '${criterion.fieldKey}'" }

		val value = DbSubmissionData::value
		val valueFilter = when (criterion) {
			is SearchCriterion.TextContains -> value
				.regex(".*${Pattern.quote(criterion.text)}.*", options = "i")
			is SearchCriterion.TextEquals -> value eq criterion.text
			is SearchCriterion.OrderAfter -> value gte criterion.min
			is SearchCriterion.OrderBefore -> value lte criterion.max
			is SearchCriterion.Exists -> null // Nothing to do, the previous loop is enough
		}

		lateinit var nodeFilter: Bson // The list 'ids' can't be empty, so this variable will always be initialized
		for (i in ids.indices.reversed()) {
			// DbSubmissionData::children is not the root of the object, so we need to have a special case for the root
			val children =
				if (i == 0) DbSubmission::data / DbSubmissionData::children
				else DbSubmissionData::children

			val keyFilter = DbSubmissionData::key eq ids[i]

			// The last id matches for the value, other ids match for the previous child
			nodeFilter =
				if (i == ids.lastIndex) children.elemMatch(and(keyFilter, valueFilter))
				else children.elemMatch(and(keyFilter, nodeFilter))
		}
		filter += nodeFilter
	}

	return submissions.find(and(filter)).toList()
}
