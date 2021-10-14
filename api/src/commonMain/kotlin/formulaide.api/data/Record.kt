package formulaide.api.data

import formulaide.api.search.SearchCriterion
import formulaide.api.types.Ref
import formulaide.api.types.Referencable
import formulaide.api.types.ReferenceId
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
	override val id: ReferenceId,
	val form: Ref<Form>,
	val state: RecordState,
	val history: List<RecordStateTransition>,
) : Referencable {

	// Left for backward compatibility
	val submissions: List<Ref<FormSubmission>>
		get() = history.mapNotNull { it.fields }

	fun load() {
		history.asSequence()
			.flatMap { sequenceOf(it.previousState, it.nextState) }
			.plus(state)
			.forEach {
				if (it is RecordState.Action)
					it.current.loadFrom(form.obj.actions, lazy = true)
			}
	}

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
	 * One of the review steps failed.
	 *
	 * To understand why, see [Record.history].
	 */
	@SerialName("FAILED")
	@Serializable
	object Refused : RecordState() {

		override fun toString() = "formulaide.api.data.RecordState\$Refused"
	}
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
	val fields: Ref<FormSubmission>? = null,
) {

	init {
		if (nextState is RecordState.Refused && previousState !is RecordState.Refused)
			requireNotNull(reason) { "Il est obligatoire de donner une raison pour fermer un dossier" }

		if (previousState != null)
			requireNotNull(assignee) { "Les utilisateurs anonymes ne peuvent pas changer l'état d'un enregistrement" }
	}
}

//region API request objects

@Serializable
data class RecordsToReviewRequest(
	val form: Ref<Form>,
	val state: RecordState?,
	val query: Map<ReferenceId?, List<SearchCriterion<*>>>,
)

@Serializable
data class ReviewRequest(
	val record: Ref<Record>,
	val transition: RecordStateTransition,
	val fields: FormSubmission?,
)

//endregion
