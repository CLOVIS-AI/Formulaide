package formulaide.db.document

import formulaide.api.data.*
import formulaide.api.search.SearchCriterion
import formulaide.api.types.Ref
import formulaide.api.types.ReferenceId
import formulaide.db.Database
import formulaide.db.document.DbSubmissionData.Companion.rootId
import formulaide.db.document.DbSubmissionData.Companion.toApi
import formulaide.db.document.DbSubmissionData.Companion.toDbSubmissionData
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
	val key: String,
	val value: String? = null,
	val children: List<DbSubmissionData> = emptyList(),
	val isList: Boolean = false,
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
				println("${parentKey.joinToString(separator = ":")}:${data.key} -> $data")

				if (data.value != null) {
					println("\tValue: ${data.value}")
					map[(parentKey + data.key).joinToString(separator = ":")] = data.value
				}

				if (data.children.isNotEmpty()) {
					for (child in data.children.filter { !it.isList }) {
						println("\tChild: $child")
						convert(child, parentKey + data.key)
					}

					val listChildren = data.children.filter { it.isList }.groupBy { it.key }
					for ((key, children) in listChildren) {
						println("\tList children: $children")

						val editedChildren = children
							.mapIndexed { i, it -> it.copy(key = i.toString(), isList = false) }
						println("\tEdited children: $editedChildren")

						val list = DbSubmissionData(key, children = editedChildren)
						convert(list, parentKey)
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
		val ids = criterion.fieldKey
			.split(":")

		var node = DbSubmission::data / DbSubmissionData::children
		for ((i, id) in ids.withIndex()) {
			filter += node / DbSubmissionData::key eq id

			if (i != ids.lastIndex)
				node /= DbSubmissionData::children
		}

		@Suppress("UNUSED_VARIABLE") // to force exhaustive when
		val e = when (criterion) {
			is SearchCriterion.TextContains -> {
				filter += (node / DbSubmissionData::value).regex(".*${Pattern.quote(criterion.text)}.*",
				                                                 options = "i")
			}
			is SearchCriterion.TextEquals -> filter += node / DbSubmissionData::value eq criterion.text
			is SearchCriterion.OrderAfter -> filter += node / DbSubmissionData::value gte criterion.min
			is SearchCriterion.OrderBefore -> filter += node / DbSubmissionData::value lte criterion.max
			is SearchCriterion.Exists -> {
			} // Nothing to do, the previous loop is enough
		}
	}

	return submissions.find(*filter.toTypedArray()).toList()
}
