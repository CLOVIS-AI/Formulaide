package formulaide.db.document

import formulaide.core.User
import formulaide.core.field.Field
import formulaide.core.field.LocalFieldId
import formulaide.core.field.resolve
import formulaide.core.form.Form
import formulaide.core.form.Submission
import formulaide.core.form.Template
import formulaide.core.record.Record
import formulaide.core.record.RecordBackbone
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import opensavvy.backbone.Ref
import opensavvy.backbone.Ref.Companion.expire
import opensavvy.backbone.Ref.Companion.requestValue
import opensavvy.backbone.RefCache
import opensavvy.state.*
import opensavvy.state.Slice.Companion.successful
import org.litote.kmongo.*
import org.litote.kmongo.coroutine.CoroutineCollection

@Serializable
class DbRecord2(
	@SerialName("_id") val id: Id<DbRecord2>,

	val formId: Id<Form>,
	val formVersion: Instant,
	val currentStep: Int?,

	val createdAt: Instant,
	val modifiedAt: Instant,

	val submissions: List<DbSubmission2>,
)

@Serializable
data class DbSubmission2(
	val forStep: Int?,

	val author: Id<DbUser>?,
	val createdAt: Instant,

	val decision: Record.Decision?,
	val reason: String?,

	val data: DbSubmissionData2?,
	val importedData: List<DbModelSubmission2>,
)

@Serializable
data class DbModelSubmission2(
	val templateId: Id<Template>,
	val templateVersion: String,
	val data: DbSubmissionData2,
)

@Serializable
data class DbSubmissionData2(
	/** The ID of this field. `null` for the root. */
	@SerialName("i") val id: LocalFieldId?,
	@SerialName("v") val value: String? = null,
	@SerialName("c") val children: List<DbSubmissionData2> = emptyList(),
) {

	init {
		for (child in children) {
			requireNotNull(child.id) { "Seule la racine peut omettre l'identifiant, mais le champ $child n'a pas d'identifiant alors qu'il fait partie du champ $this" }
		}
	}
}

private fun Submission.toDb(fields: Field.Container): DbSubmissionData2 {
	verify(fields)

	fun convert(id: Field.Id, field: Field, mandatory: Boolean): DbSubmissionData2 {
		return when (field) {
			is Field.Label -> DbSubmissionData2(
				id = id.parts.lastOrNull(),
			)

			is Field.Input -> DbSubmissionData2(
				id = id.parts.lastOrNull(),
				value = data[id]
			)

			is Field.Choice -> DbSubmissionData2(
				id = id.parts.lastOrNull(),
				value = data[id],
				children = field.indexedFields.map { (subId, subField) ->
					convert(id + subId, subField, mandatory = mandatory)
				}
			)

			is Field.Group -> DbSubmissionData2(
				id = id.parts.lastOrNull(),
				children = field.indexedFields.map { (subId, subField) ->
					convert(id + subId, subField, mandatory = mandatory)
				}
			)

			is Field.List -> DbSubmissionData2(
				id = id.parts.lastOrNull(),
				children = field.indexedFields.map { (subId, subField) ->
					convert(id + subId, subField, mandatory = mandatory && subId < field.allowed.first.toInt())
				}
			)
		}
	}

	return convert(Field.Id.root, fields.root, mandatory = true)
}

private fun DbSubmissionData2.toCore(): Submission {
	val data = HashMap<Field.Id, String>()

	fun convert(parent: Field.Id, answer: DbSubmissionData2) {
		val me = if (answer.id != null)
			parent + answer.id
		else
			Field.Id.root

		if (answer.value != null)
			data[me] = answer.value

		for (child in answer.children)
			convert(me, child)
	}

	convert(Field.Id.root, this)

	return Submission(data)
}

class Records(
	private val records: CoroutineCollection<DbRecord2>,
	private val forms: Forms,
	private val users: Users,
	override val cache: RefCache<Record>,
) : RecordBackbone {
	override suspend fun create(form: Form.Ref, version: Instant, user: User.Ref?, submission: Submission): Record.Ref {
		val requestedForm = form.requestValue()

		val requestedVersion = requestedForm.versions.find { it.creationDate == version }
		requireNotNull(requestedVersion) { "La version sélectionnée ($version) du formulaire ${form.id} (${requestedForm.name}) ne correspond à aucune version existante : ${requestedForm.versions}" }

		val fields = requestedVersion.fields.requestValue().resolve()
		val now = Clock.System.now()

		val record = DbRecord2(
			id = newId(),
			formId = form.id.toId(),
			formVersion = version,
			currentStep = requestedVersion.reviewSteps.minBy { it.id }.id,
			createdAt = now,
			modifiedAt = now,
			submissions = listOf(
				DbSubmission2(
					forStep = null,
					author = user?.email?.toId(),
					createdAt = now,
					decision = null,
					reason = null,
					data = submission.toDb(fields),
					importedData = emptyList(), //TODO in #124
				)
			)
		)

		records.insertOne(record)

		return Record.Ref(record.id.toString(), this)
	}

	override suspend fun editInitial(record: Record.Ref, user: User.Ref, submission: Submission) {
		val requestedRecord = records.findOneById(record.id)
			?: error("Impossible de modifier la saisie initiale d'un dossier n'existant pas : $record")

		val requestedForm = forms.fromId(requestedRecord.formId.toString()).requestValue()
		val requestedVersion = requestedForm.versions.find { it.creationDate == requestedRecord.formVersion }
			?: error("Impossible de trouver la version ${requestedRecord.formVersion} du formulaire $requestedForm")
		val requestedFields = requestedVersion.fields.requestValue().resolve()

		val now = Clock.System.now()

		records.updateOne(
			DbRecord2::id eq record.id.toId(), combine(
				setValue(DbRecord2::modifiedAt, now),
				push(
					DbRecord2::submissions, DbSubmission2(
						forStep = null,
						author = user.email.toId(),
						createdAt = now,
						decision = null,
						reason = null,
						data = submission.toDb(requestedFields),
						importedData = emptyList(), //TODO in #124
					)
				)
			)
		)

		record.expire()
	}

	override suspend fun review(
		record: Record.Ref,
		user: User.Ref,
		step: Int?,
		decision: Record.Decision,
		reason: String?,
		submission: Submission?,
	) {
		if (decision is Record.Decision.Refused)
			requireNotNull(reason) { "Il est obligatoire de fournir une raison quand on refuse un dossier" }

		val requestedRecord = records.findOneById(record.id)
			?: error("Impossible de modifier la saisie initiale d'un dossier n'existant pas : $record")

		val requestedForm = forms.fromId(requestedRecord.formId.toString()).requestValue()
		val requestedVersion = requestedForm.versions.find { it.creationDate == requestedRecord.formVersion }
			?: error("Impossible de trouver la version ${requestedRecord.formVersion} du formulaire $requestedForm")
		val requestedFields = requestedVersion.reviewSteps.find { it.id == requestedRecord.currentStep }
			?: error("Impossible de trouver l'étape de validation dans laquelle se trouve le dossier $record (${requestedRecord.currentStep}) dans le formulaire $requestedForm")

		val now = Clock.System.now()

		records.updateOne(
			DbRecord2::id eq record.id.toId(), combine(
				setValue(DbRecord2::modifiedAt, now),
				push(
					DbRecord2::submissions, DbSubmission2(
						forStep = step,
						author = user.email.toId(),
						createdAt = now,
						decision = decision,
						reason = reason,
						data = if (requestedFields.fields != null) submission!!.toDb(
							requestedFields.fields!!.requestValue().resolve()
						) else null,
						importedData = emptyList(), //TODO in #124
					)
				)
			)
		)

		record.expire()
	}

	override suspend fun list(): List<Record.Ref> {
		return this.records.find()
			.projection(DbRecord2::id)
			.toList()
			.map { Record.Ref(it.id.toString(), this) }
	}

	fun fromId(id: String) = Record.Ref(id, this)

	override fun directRequest(ref: Ref<Record>): State<Record> = state {
		ensureValid(ref is Record.Ref) { "${this@Records} doesn't support the reference $ref" }

		val result = records.findOne(DbRecord2::id eq ref.id.toId())
		ensureFound(result != null) { "Le dossier ${ref.id} est introuvable" }

		val output = Record(
			id = result.id.toString(),
			form = forms.fromId(result.formId.toString()),
			formVersion = result.formVersion,
			currentStep = result.currentStep,
			createdAt = result.createdAt,
			modifiedAt = result.modifiedAt,
			snapshots = result.submissions.map { submission ->
				Record.Snapshot(
					author = submission.author?.let { users.fromId(it.toString()) },
					forStep = submission.forStep,
						decision = submission.decision,
						reason = submission.reason,
						createdAt = submission.createdAt,
						submission = submission.data?.toCore()
					)
				}
			)

		emit(successful(output))
	}
}
