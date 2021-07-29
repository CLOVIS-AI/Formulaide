package formulaide.db.document

import formulaide.api.data.*
import formulaide.api.types.Ref
import formulaide.api.types.Ref.Companion.createRef
import formulaide.api.types.Ref.Companion.load
import formulaide.db.Database
import org.litote.kmongo.div
import org.litote.kmongo.eq
import org.litote.kmongo.newId
import java.time.Instant

suspend fun Database.createRecord(submission: FormSubmission) {
	submission.form.load { findForm(it) ?: error("Impossible de trouver le formulaire $it") }
	val form = submission.form.obj

	val state =
		form.actions.firstOrNull()?.let { RecordState.Action(it.createRef()) } ?: RecordState.Done

	val record = Record(
		newId<Record>().toString(),
		form.createRef(),
		state,
		submissions = listOf(submission.createRef()),
		history = listOf(RecordStateTransition(
			Instant.now().epochSecond,
			previousState = null,
			nextState = state,
			assignee = null,
			reason = "Saisie originelle",
		))
	)

	records.insertOne(record)
}

suspend fun Database.findFormsAssignedTo(user: DbUser): List<Form> {
	val service = findService(user.service)
		?: error("Impossible de trouver le service à qui cet utilisateur appartient : ${user.service}")

	return forms.find(Form::actions / Action::reviewer / Ref<*>::id eq service.id.toString())
		.toList()
}

suspend fun Database.findRecords(form: Form, state: RecordState): List<Record> =
	records.find(Record::form / Ref<*>::id eq form.id, Record::state eq state)
		.limit(Record.MAXIMUM_NUMBER_OF_RECORDS_PER_ACTION)
		.toList()
