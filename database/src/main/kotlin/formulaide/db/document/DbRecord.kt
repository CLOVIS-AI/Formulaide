package formulaide.db.document

import formulaide.api.data.*
import formulaide.api.types.Ref
import formulaide.api.types.Ref.Companion.SPECIAL_TOKEN_NEW
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

suspend fun Database.reviewRecord(review: ReviewRequest, employee: DbUser) {
	val record = records.findOne(Record::id eq review.record.id)
		?: error("Impossible de trouver le dossier ${review.record.id}")
	record.form.load {
		findForm(it) ?: error("Impossible de trouver le formulaire ${record.form.id}")
	}

	require(record.state == review.transition.previousState) { "Il n'est pas possible de transférer le dossier depuis l'étape ${review.transition.previousState} alors qu'il est actuellement dans l'état ${record.state}" }
	require(employee.email == review.transition.assignee?.id) { "Il est interdit d'attribuer la modification à quelqu'un d'autre que soit-même" }
	require(review.transition.timestamp > record.history.maxOf { it.timestamp }) { "Une modification doit être plus récente que toutes les modifications déjà appliquées" }

	val submissionToCreate: DbSubmission?
	val previous = review.transition.previousState
	val next = review.transition.nextState
	when {
		next is RecordState.Refused -> {
			require(review.fields == null) { "Une transition depuis l'état ${RecordState.Refused} ne peut pas contenir de champs" }

			submissionToCreate = null
		}
		previous is RecordState.Action -> {
			previous.current.loadFrom(record.form.obj.actions, lazy = false, allowNotFound = false)

			val submission = review.fields
				?: FormSubmission(SPECIAL_TOKEN_NEW,
				                  record.form,
				                  previous.current,
				                  emptyMap())
			submissionToCreate = saveSubmission(submission)
		}
		else -> {
			require(review.fields == null) { "Une transition depuis l'état ${RecordState.Done} ou ${RecordState.Refused} ne peut pas contenir de champs" }

			submissionToCreate = null
		}
	}

	val newRecord = record.copy(
		state = review.transition.nextState,
		submissions = record.submissions +
				(if (submissionToCreate != null) listOf(submissionToCreate.toApi().createRef())
				else emptyList()),
		history = record.history + review.transition
	)

	records.updateOne(Record::id eq record.id, newRecord)
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
