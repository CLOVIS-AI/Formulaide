package formulaide.db.document

import formulaide.api.bones.canAccess
import formulaide.api.data.*
import formulaide.api.types.Ref
import formulaide.api.types.Ref.Companion.createRef
import formulaide.api.types.Ref.Companion.load
import formulaide.core.User
import formulaide.db.Database
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import opensavvy.backbone.Ref.Companion.requestValue
import org.bson.conversions.Bson
import org.litote.kmongo.*
import java.time.Instant
import java.util.*

suspend fun Database.createRecord(submission: FormSubmission, userEmail: String? = null) {
	submission.form.load { findLegacyForm(it) ?: error("Impossible de trouver le formulaire $it") }
	val form = submission.form.obj

	val state =
		form.actions.firstOrNull()?.let { RecordState.Action(it.createRef()) }
			?: error("Le formulaire ${form.id} n'a pas d'étapes, il n'est pas possible de créer une saisie")

	val record = Record(
		newId<Record>().toString(),
		form.createRef(),
		state,
		history = listOf(
			RecordStateTransition(
				Instant.now().epochSecond,
				previousState = null,
				nextState = state,
				assignee = userEmail?.let { Ref(it) },
				reason = "Saisie initiale",
				fields = submission.createRef(),
			)
		)
	)

	records.insertOne(record)
}

suspend fun Database.findRecord(record: Ref<Record>): Record? =
	records.findOne(Record::id eq record.id)

suspend fun Database.reviewRecord(review: ReviewRequest, employee: User) {
	val record = records.findOne(Record::id eq review.record.id)
		?: error("Impossible de trouver le dossier ${review.record.id}")
	record.form.load {
		findLegacyForm(it) ?: error("Impossible de trouver le formulaire ${record.form.id}")
	}
	val transition = review.transition
		.copy(timestamp = Instant.now().epochSecond)

	require(record.state == transition.previousState) { "Il n'est pas possible de transférer le dossier depuis l'étape ${transition.previousState} alors qu'il est actuellement dans l'état ${record.state}" }
	require(employee.email == transition.assignee?.id) { "Il est interdit d'attribuer la modification à quelqu'un d'autre que soit-même" }
	require(transition.timestamp > record.history.maxOf { it.timestamp }) { "Une modification doit être plus récente que toutes les modifications déjà appliquées" }
	require(
		employee.canAccess(
			record.form.obj,
			record.state
		)
	) { "Vous ne pouvez pas prendre une décision sur un dossier sans être assigné(e) à l'étape dans laquelle il se trouve" }

	val submissionToCreate: DbSubmission?
	val previous = transition.previousState
	val next = transition.nextState
	when {
		next is RecordState.Refused -> {
			require(review.fields == null) { "Une transition vers l'état ${RecordState.Refused} ne peut pas contenir de champs" }

			submissionToCreate = null
		}
		previous is RecordState.Action -> {
			previous.current.loadFrom(record.form.obj.actions, lazy = false, allowNotFound = false)

			submissionToCreate = review.fields?.let { saveLegacySubmission(it) }
		}
		previous is RecordState.Refused -> {
			require(review.fields == null) { "Une transition depuis l'état ${RecordState.Refused} ne peut pas contenir de champs" }

			submissionToCreate = null
		}
		else -> error("Situation impossible : l'étape précédente n'est ni 'refusé' ni une action, trouvé $previous")
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
	return coroutineScope {
		user.services
			.ifEmpty {
				@Suppress("DEPRECATION") // This is the fallback in case the new field is empty
				user.service?.let { listOf(it) } ?: emptyList()
			}
			.map {
				async {
					val service = departments.fromId(it).requestValue()

					legacyForms.find(Form::actions / Action::reviewer / Ref<*>::id eq service.id).toList()
				}
			}
			.awaitAll()
	}.flatten()
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
	val submissionFilter = mutableListOf<Bson>()
	for ((action, subs) in (submissions ?: emptyList()).groupBy { it.root }) {
		val filterOneOfIds = (RecordStateTransition::fields / Ref<*>::id).`in`(subs.map { it.apiId })

		if (action == null) {
			submissionFilter.add(
				Record::history.elemMatch(
					and(
						filterOneOfIds,
						RecordStateTransition::previousState eq null,
					)
				)
			)
		} else {
			submissionFilter.add(
				Record::history.elemMatch(
					and(
						filterOneOfIds,
						RecordStateTransition::previousState eq RecordState.Action(Ref(action)),
					)
				)
			)
		}
	}

	val stateFilter = (Record::state eq state)
		.takeIf { state != null }

	var results = records
		.find(Record::form / Ref<*>::id eq form.id, stateFilter, and(submissionFilter))
		.descendingSort(Record::id)

	if (limit != null)
		results = results.limit(limit)

	return results.toList()
}

suspend fun Database.deleteRecord(record: Record, user: DbUser) {
	createAlert(
		Alert(
			level = Alert.Level.High,
			timestamp = Date.from(Instant.now()).time,
			message = "L'utilisateur ${user.email} a supprimé le dossier ${record.id}. Veuillez vérifier que ce n'est pas un accident. S'il s'agit d'une attaque, la seule manière de récupérer les informations est de restaurer une sauvegarde de la base de données.",
			user = Ref(user.email)
		)
	)

	for (transition in record.history) {
		transition.fields?.let {
			deleteLegacySubmission(it.id)
		}
	}

	records.deleteOne(Record::id eq record.id)
}
