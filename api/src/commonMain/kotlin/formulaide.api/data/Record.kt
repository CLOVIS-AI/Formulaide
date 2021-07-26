package formulaide.api.data

import formulaide.api.types.Ref
import formulaide.api.users.User
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import formulaide.api.data.Action as FormAction

/**
 * The administrative record of the request submitted to a [Form] through the different [review steps][RecordState].
 *
 * @property form The form this record corresponds to. A record can be valid even if the form is closed.
 * @property state The current [state][RecordState] of this record.
 * @property submissions The different submissions attached to this form.
 */
@Serializable
data class Record(
	val form: Ref<Form>,
	val state: RecordState,
	val submissions: List<Ref<FormSubmission>>,
	val history: List<RecordStateTransition>,
) {

	companion object {
		const val MAXIMUM_NUMBER_OF_RECORDS_PER_ACTION = 100
	}
}

/**
 * The current state of a [Record].
 */
@Serializable
sealed class RecordState {

	/**
	 * The record is currently waiting for the [current] action to be verified.
	 */
	@SerialName("ACTION")
	@Serializable
	data class Action(val current: Ref<FormAction>) : RecordState()

	/**
	 * All review steps have been successfully validated.
	 */
	@SerialName("DONE")
	@Serializable
	object Done : RecordState()

	/**
	 * One of the review steps failed.
	 *
	 * To understand why, see [Record.history].
	 */
	@SerialName("FAILED")
	@Serializable
	object Refused : RecordState()
}

/**
 * One of the state transitions that happened to a [Record].
 *
 * @property timestamp When the transition happened.
 * @property assignee The employee responsible for that transition.
 */
@Serializable
data class RecordStateTransition(
	val timestamp: Long,
	val previousState: RecordState?,
	val nextState: RecordState,
	val assignee: Ref<User>?,
	val reason: String?,
) {

	init {
		if (nextState is RecordState.Refused)
			requireNotNull(reason) { "Il est obligatoire de donner une raison pour fermer un dossier" }

		if (previousState != null)
			requireNotNull(assignee) { "Les utilisateurs anonymes ne peuvent pas changer l'état d'un enregistrement" }
	}
}

@Serializable
data class RecordsToReviewRequest(
	val form: Ref<Form>,
	val state: RecordState,
)
