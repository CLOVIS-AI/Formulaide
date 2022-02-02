package formulaide.db.document

import formulaide.api.data.*
import formulaide.api.types.Ref
import formulaide.api.types.Ref.Companion.createRef
import formulaide.api.types.Ref.Companion.load
import formulaide.db.Database
import org.litote.kmongo.*
import java.time.Instant

suspend fun Database.createRecord(submission: FormSubmission) {
	submission.form.load { findForm(it) ?: error("Impossible de trouver le formulaire $it") }
	val form = submission.form.obj

	val state =
		form.actions.firstOrNull()?.let { RecordState.Action(it.createRef()) }
			?: error("Le formulaire ${form.id} n'a pas d'étapes, il n'est pas possible de créer une saisie")

	val record = Record(
		newId<Record>().toString(),
		form.createRef(),
		state,
		history = listOf(RecordStateTransition(
			Instant.now().epochSecond,
			previousState = null,
			nextState = state,
			assignee = null,
			reason = "Saisie initiale",
			fields = submission.createRef(),
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
	val transition = review.transition
		.copy(timestamp = Instant.now().epochSecond)

	require(record.state == transition.previousState) { "Il n'est pas possible de transférer le dossier depuis l'étape ${transition.previousState} alors qu'il est actuellement dans l'état ${record.state}" }
	require(employee.email == transition.assignee?.id) { "Il est interdit d'attribuer la modification à quelqu'un d'autre que soit-même" }
	require(transition.timestamp > record.history.maxOf { it.timestamp }) { "Une modification doit être plus récente que toutes les modifications déjà appliquées" }

	val submissionToCreate: DbSubmission?
	val previous = transition.previousState
	val next = transition.nextState
	when {
		next is RecordState.Refused -> {
			require(review.fields == null) { "Une transition depuis l'état ${RecordState.Refused} ne peut pas contenir de champs" }

			submissionToCreate = null
		}
		previous is RecordState.Action -> {
			previous.current.loadFrom(record.form.obj.actions, lazy = false, allowNotFound = false)

			submissionToCreate = review.fields?.let { saveSubmission(it) }
		}
		else -> {
			require(review.fields == null) { "Une transition depuis l'état ${RecordState.Refused} ne peut pas contenir de champs" }

			submissionToCreate = null
		}
	}

	val newRecord = record.copy(
		state = transition.nextState,
		history = record.history +
				(if (submissionToCreate != null)
					transition.copy(fields = Ref(submissionToCreate.apiId))
				else
					transition)
	)

	records.updateOne(Record::id eq record.id, newRecord)
}

suspend fun Database.findFormsAssignedTo(user: DbUser): List<Form> {
	val service = findService(user.service)
		?: error("Impossible de trouver le service à qui cet utilisateur appartient : ${user.service}")

	return forms.find(Form::actions / Action::reviewer / Ref<*>::id eq service.id.toString())
		.toList()
}

/**
 * Find records corresponding to a given [form].
 *
 * @param state If not `null`, this function will only return records that are currently in that [state].
 * @param submissions If not `null`, only returns records that contain at least one of these [submissions].
 * @param limit If not `null`, return at most [limit] results.
 */
suspend fun Database.findRecords(
	form: Form,
	state: RecordState?,
	submissions: List<DbSubmission>? = null,
	limit: Int? = Record.MAXIMUM_NUMBER_OF_RECORDS_PER_ACTION,
): List<Record> {
	val submissionsFilter = submissions
		?.map { it.apiId }
		?.let { (Record::history / RecordStateTransition::fields / Ref<*>::id).`in`(it) }

	val stateFilter = (Record::state eq state)
		.takeIf { state != null }

	var results = records
		.find(Record::form / Ref<*>::id eq form.id, stateFilter, submissionsFilter)

	if (limit != null)
		results = results.limit(limit)

	return results.toList()
}
