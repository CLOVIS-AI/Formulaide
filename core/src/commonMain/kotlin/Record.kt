package opensavvy.formulaide.core

import arrow.core.Nel
import kotlinx.datetime.Instant
import opensavvy.backbone.Backbone
import opensavvy.formulaide.core.Submission.Companion.toSubmissionData
import opensavvy.formulaide.core.data.StandardNotFound
import opensavvy.formulaide.core.data.StandardUnauthenticated
import opensavvy.formulaide.core.data.StandardUnauthorized
import opensavvy.state.outcome.Outcome

/**
 * A user request and its modifications over time.
 */
data class Record(
	/**
	 * The form this record was created against.
	 */
	val form: Form.Version.Ref,
	val createdAt: Instant,
	val modifiedAt: Instant,

	/**
	 * The history of this record.
	 */
	val history: List<Diff>,
) {

	val historySorted by lazy(LazyThreadSafetyMode.PUBLICATION) { history.sortedBy { it.at } }

	val status by lazy(LazyThreadSafetyMode.PUBLICATION) { historySorted.last().target }

	init {
		require(history.isNotEmpty()) { "Un dossier ne peut pas être créé avant la saisie initiale" }
		require(history.first() is Diff.Initial) { "Un dossier doit obligatoirement commencer par une saisie initiale : $this" }
		require(history.count { it is Diff.Initial } == 1) { "Un dossier doit avoir exactement une saisie initiale : $this" }
	}

	val initialSubmission get() = history.first() as Diff.Initial // checked by the constructor

	/**
	 * The current state of a [Record] in a form's [automaton][Form.Step].
	 */
	sealed interface Status {
		/** Marker interface for status that do not allow [Initial]. */
		sealed interface NonInitial : Status

		/**
		 * The record was just created.
		 *
		 * This status can never appear in [Record.status], as records are automatically advanced to the first step.
		 * However, it may appear in [Diff.source].
		 */
		object Initial : Status

		/**
		 * @property step The [identifier][Form.Step.id] of a [form step][Form.Step].
		 */
		data class Step(val step: Int) : Status, NonInitial

		/**
		 * The record has been refused.
		 *
		 * To un-refuse this record, use a [Diff.MoveBack] transition.
		 */
		object Refused : Status, NonInitial
	}

	/**
	 * A transition in a form's [automaton][Form.Step].
	 *
	 * A diff represents the transition from a [source] status to a [target] status, made [at] a specific point in time.
	 *
	 * A diff records its [author], the [reason] it was made, as well as a newly-created [submission].
	 */
	sealed class Diff {

		abstract val at: Instant

		/**
		 * The author of this [Diff].
		 *
		 * This field can only be `null` if the [Initial] submission is created by a guest.
		 */
		abstract val author: User.Ref?

		/**
		 * The [Status] a record was in when this diff was created.
		 *
		 * If a [submission] is given, this is the state against which it applies.
		 */
		abstract val source: Status

		/**
		 * The [Status] a record is in immediately after this diff is created.
		 */
		abstract val target: Status.NonInitial

		/**
		 * A submission created against the [source] status.
		 */
		abstract val submission: Submission.Ref?

		/**
		 * A user-provided reason for this decision.
		 */
		abstract val reason: String?

		/**
		 * The diff each record starts with, representing the initial user's submission.
		 */
		data class Initial(
			override val submission: Submission.Ref,
			override val author: User.Ref?,
			val firstStep: Int,
			override val at: Instant,
		) : Diff() {

			/** An initial submission always has the status [Initial][Status.Initial]. */
			override val source get() = Status.Initial

			/** An initial submission always sends the record to the [first step][firstStep]. */
			override val target get() = Status.Step(firstStep)

			/** No reason can be given when creating an initial submission, so this value is always `null`. */
			override val reason get() = null
		}

		/**
		 * A transition allowing to shadow the user's real [initial submission][Initial], useful when mistakes are spotted
		 * after the fact.
		 *
		 * Because this is only a transition, the original transition which created the record is still available in its
		 * history, so no data is lost.
		 *
		 * This transition has no impact on the record's status, so [source] and [target] both return [currentStatus].
		 */
		data class EditInitial(
			override val submission: Submission.Ref,
			override val author: User.Ref,
			override val reason: String,
			val currentStatus: Status.NonInitial,
			override val at: Instant,
		) : Diff() {

			override val source get() = currentStatus
			override val target get() = currentStatus
		}

		/**
		 * A transition created when [author] accepts this record for the [current][source] status.
		 */
		data class Accept(
			override val submission: Submission.Ref?,
			override val author: User.Ref,
			override val source: Status.Step,
			override val target: Status.Step,
			override val reason: String?,
			override val at: Instant,
		) : Diff() {

			init {
				require(source.step < target.step) { "Une acceptation doit obligatoirement faire avancer le dossier, trouvé une transition de l'étape ${source.step} à ${target.step}" }
			}
		}

		/**
		 * A transition created when [author] refuses this record for the [current][source] status.
		 */
		data class Refuse(
			override val author: User.Ref,
			override val source: Status.Step,
			override val reason: String,
			override val at: Instant,
		) : Diff() {

			override val target get() = Status.Refused
			override val submission get() = null
		}

		/**
		 * Allows the [author] to update the answers to the [current][source] status, without changing it.
		 *
		 * This is used to update the [reason] or provide a new [submission].
		 *
		 * This transition has no impact on the record's status, so [source] and [target] both return [currentStatus].
		 */
		data class EditCurrent(
			override val submission: Submission.Ref?,
			override val author: User.Ref,
			val currentStatus: Status.NonInitial,
			override val reason: String?,
			override val at: Instant,
		) : Diff() {

			override val source get() = currentStatus
			override val target get() = currentStatus
		}

		/**
		 * Allows the [author] to move a record to a state it was previously in.
		 *
		 * This transition is also used to cancel a [Refuse] transition.
		 */
		data class MoveBack(
			override val author: User.Ref,
			override val source: Status.NonInitial,
			override val target: Status.Step,
			override val reason: String,
			override val at: Instant,
		) : Diff() {

			init {
				if (source is Status.Step) {
					require(source.step > target.step) { "Une transition en arrière ne peut aller vers un état futur, trouvé une transition de ${source.step} à ${target.step}" }
				}
			}

			override val submission get() = null
		}
	}

	sealed class QueryCriterion

	interface Ref : opensavvy.backbone.Ref<Failures.Get, Record> {

		/**
		 * Creates a [Diff.EditInitial] for this record.
		 */
		suspend fun editInitial(
			reason: String,
			submission: Map<Field.Id, String>,
		): Outcome<Failures.Action, Unit>

		/**
		 * Creates a [Diff.EditInitial] for this record.
		 */
		suspend fun editInitial(
			reason: String,
			vararg submission: Pair<String, String>,
		) = editInitial(reason, submission.toSubmissionData())

		/**
		 * Creates a [Diff.EditCurrent] for this record.
		 */
		suspend fun editCurrent(
			reason: String?,
			submission: Map<Field.Id, String>,
		): Outcome<Failures.Action, Unit>

		/**
		 * Creates a [Diff.EditCurrent] for this record.
		 */
		suspend fun editCurrent(
			reason: String?,
			vararg submission: Pair<String, String>,
		) = editCurrent(reason, submission.toSubmissionData())

		/**
		 * Creates a [Diff.Accept] for this record.
		 */
		suspend fun accept(
			reason: String?,
			submission: Map<Field.Id, String>,
		): Outcome<Failures.Action, Unit>

		/**
		 * Creates a [Diff.Accept] for this record.
		 */
		suspend fun accept(
			reason: String?,
			vararg submission: Pair<String, String>,
		) = accept(reason, submission.toSubmissionData())

		/**
		 * Creates a [Diff.Refuse] for this record.
		 */
		suspend fun refuse(
			reason: String,
		): Outcome<Failures.Action, Unit>

		/**
		 * Creates a [Diff.MoveBack] for this record.
		 */
		suspend fun moveBack(
			toStep: Int,
			reason: String,
		): Outcome<Failures.Action, Unit>
	}

	interface Service : Backbone<Ref, Failures.Get, Record> {

		suspend fun search(criteria: List<QueryCriterion>): Outcome<Failures.Search, List<Ref>>

		suspend fun search(vararg criteria: QueryCriterion) = search(criteria.asList())

		suspend fun create(submission: Submission): Outcome<Failures.Create, Ref>

		suspend fun create(form: Form.Version.Ref, vararg data: Pair<String, String>) = create(
			Submission(
				form = form,
				formStep = null,
				data = data.toSubmissionData(),
			)
		)
	}

	sealed interface Failures {
		sealed interface Get : Failures, Action
		sealed interface Search : Failures
		sealed interface Action : Failures
		sealed interface Create : Failures

		data class NotFound(override val id: Ref) : StandardNotFound<Ref>,
			Get,
			Action

		object Unauthenticated : StandardUnauthenticated,
			Get,
			Search,
			Action,
			Create

		object Unauthorized : StandardUnauthorized,
			Get,
			Search,
			Action

		object CannotCreateRecordForNonInitialStep : Create

		data class FormNotFound(
			override val id: Form.Ref,
			val form: Form.Failures.Get?,
		) : StandardNotFound<Form.Ref>,
			Create

		class FormVersionNotFound(
			override val id: Form.Version.Ref,
			val form: Form.Version.Failures.Get,
		) : StandardNotFound<Form.Version.Ref>,
			Create,
			Action

		data class InvalidSubmission(
			val failure: Nel<Submission.ParsingFailure>,
		) : Action,
			Create

		data class DiffShouldStartAtCurrentState(
			val current: Status.NonInitial,
			val diffSource: Status,
		) : Action {
			override fun toString() = "A transition must start on the current state of the record ($current), but found $diffSource"
		}

		object CannotAcceptRefusedRecord : Action

		object CannotRefuseRefusedRecord : Action

		object CannotAcceptFinishedRecord : Action

		object MandatoryReason : Action

		object CannotMoveBackToFutureStep : Action
	}
}
