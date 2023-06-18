package opensavvy.formulaide.core

import arrow.core.Nel
import kotlinx.datetime.Instant
import opensavvy.backbone.Backbone
import opensavvy.formulaide.core.Form.*
import opensavvy.formulaide.core.data.StandardNotFound
import opensavvy.formulaide.core.data.StandardUnauthenticated
import opensavvy.formulaide.core.data.StandardUnauthorized
import opensavvy.formulaide.core.utils.IdentifierParser
import opensavvy.formulaide.core.utils.IdentifierWriter
import opensavvy.state.outcome.Outcome

/**
 * The schema of a user's interactions with Formulaide.
 *
 * At a very high level, Formulaide usage looks like:
 * - An administrator creates a Form.
 * They define which fields a user should fill in, as well as the verification steps taken by employees to check the
 * validity of the submission and to act on it.
 * - An end-user submits their answers to the form.
 * - Employees successively check the values or act upon them (see [Step]).
 *
 * Like [templates][Template], forms are [versioned][versions], and can be [open] or closed.
 *
 * Forms are visible to different users depending on their [visibility][public].
 */
data class Form(
	/**
	 * The human-readable name of this form.
	 *
	 * @see Ref.rename
	 */
	val name: String,

	/**
	 * The various versions of this form.
	 *
	 * @see Version
	 * @see Ref.createVersion
	 */
	val versions: List<Version.Ref>,

	/**
	 * `true` if this form is open.
	 *
	 * It is not possible to create a submission for a closed form.
	 *
	 * The visibility of a form is only dependent on [public], closed forms are visible to the same people as open forms.
	 */
	val open: Boolean,

	/**
	 * `true` if this form is public.
	 *
	 * Public forms are visible to everyone (even anonymous users).
	 * Private forms are visible to employees.
	 *
	 * Private forms can be used for testing and preparing data for the final release.
	 * Employees can submit answers to private forms (if they are [open]).
	 */
	val public: Boolean,
) {

	val versionsSorted by lazy(LazyThreadSafetyMode.PUBLICATION) { versions.sortedBy { it.creationDate } }

	interface Ref : opensavvy.backbone.Ref<Failures.Get, Form>, IdentifierWriter {

		/**
		 * Edits this form.
		 */
		suspend fun edit(name: String? = null, open: Boolean? = null, public: Boolean? = null): Outcome<Failures.Edit, Unit>

		/**
		 * Renames this form.
		 *
		 * @see Form.name
		 */
		suspend fun rename(name: String): Outcome<Failures.Edit, Unit> = edit(name = name)

		/**
		 * Opens this form.
		 *
		 * @see Form.open
		 */
		suspend fun open(): Outcome<Failures.Edit, Unit> = edit(open = true)

		/**
		 * Closes this form.
		 *
		 * @see Form.open
		 */
		suspend fun close(): Outcome<Failures.Edit, Unit> = edit(open = false)

		/**
		 * Makes this form public.
		 *
		 * @see Form.open
		 */
		suspend fun publicize(): Outcome<Failures.Edit, Unit> = edit(public = true)

		/**
		 * Makes this form private.
		 *
		 * @see Form.open
		 */
		suspend fun privatize(): Outcome<Failures.Edit, Unit> = edit(public = false)

		/**
		 * Creates a new version of this template.
		 *
		 * @see Template.versions
		 */
		suspend fun createVersion(title: String, field: Field, vararg step: Step): Outcome<Failures.CreateVersion, Version.Ref>

		// This is a compiler trick to make the vararg mandatory
		@Suppress("DeprecatedCallableAddReplaceWith")
		@Deprecated("It is not allowed to create a form with no review steps.", level = DeprecationLevel.ERROR)
		fun createVersion(title: String, field: Field): Outcome<Failures.CreateVersion, Version.Ref> =
			throw NotImplementedError("It is not allowed to create a form with no review steps.")

		/**
		 * Creates a reference to the version created at [creationDate].
		 *
		 * No verification is done that the version actually exists.
		 */
		fun versionOf(creationDate: Instant): Version.Ref
	}

	data class Version(
		/**
		 * The instant at which this version was created.
		 *
		 * This instant is used to uniquely identify a version of a specific form.
		 */
		val creationDate: Instant,

		/**
		 * The human-readable name of this version.
		 *
		 * It will never be visible by users when submitting their answers to the form,
		 * however it is visible to employees in their to-do list.
		 */
		val title: String,

		/**
		 * The fields that should be answered by the user making the original request.
		 */
		val field: Field,

		/**
		 * The different steps a user's submission goes through.
		 *
		 * Each form has at least one step.
		 */
		val steps: List<Step>,
	) {

		init {
			require(steps.isNotEmpty()) { "Un formulaire doit contenir au moins une étape" }
		}

		val stepsSorted by lazy(LazyThreadSafetyMode.PUBLICATION) { steps.sortedBy { it.id } }

		/**
		 * Finds all the fields in this version.
		 */
		val fields
			get() = sequence {
				yield(field)

				for (step in steps)
					if (step.field != null)
						yield(step.field)
			}

		/**
		 * Finds the field instance corresponding to [step].
		 *
		 * - If [step] is `null`, returns [Version.field]
		 * - Otherwise, returns [Step.field] for the field with ID [step].
		 */
		fun findFieldForStep(step: Int?) =
			if (step != null) steps.first { it.id == step }.field
				?: error("L'étape $step de cette version n'a pas de champ : $this")
			else field

		interface Ref : opensavvy.backbone.Ref<Failures.Get, Version>, IdentifierWriter {
			val creationDate: Instant
			val form: Form.Ref
		}

		interface Service : Backbone<Ref, Failures.Get, Version>, IdentifierParser<Ref>

		sealed interface Failures {
			sealed interface Get : Failures

			data class NotFound(override val id: Ref) : StandardNotFound<Ref>,
				Get

			data class CouldNotGetForm(val error: Form.Failures.Get) : Get

			object Unauthenticated : StandardUnauthenticated,
				Get
		}
	}

	/**
	 * Schema of the verification or action a [reviewer] should make about this [Form].
	 *
	 * Steps are organized as automata / state machines.
	 * Steps are uniquely identified and sorted via their [id].
	 *
	 * At each step, the [reviewer] is asked to accept or refuse a submission.
	 * Accepting the submission moves it to the next step.
	 *
	 * For example, let's imagine a form with the following steps:
	 * - ① Check the validity of the submitted file scans,
	 * - ② Call the user to decide on a meeting date,
	 * - ③ After the meeting has passed, finish the process.
	 *
	 * ```
	 *     Initial submission  →  ①  →  ②  →  ③
	 * ```
	 *
	 * The "Initial submission" state is imaginary.
	 * The creation of a submission by a user is the transition between "Initial submission" and the first step.
	 * Users can see the list of submissions awaiting an answer in steps they are assigned to.
	 * - If they accept the submission, it moves to the next step.
	 * - If they ask for more information, it either stays in the next step or goes to a previous one.
	 * - If they refuse the submission, it moves to the 'refused' imaginary trap step.
	 * Administrators should never create a failure state themselves.
	 *
	 * The final step is the step with the greatest [id].
	 * It is therefore often a good idea to create your own "Final" step.
	 *
	 * Steps always form a single branch. It is not possible to create diverging steps.
	 * However, it is possible to go back to a previous step.
	 */
	data class Step(
		/**
		 * Unique identifier of this step in this version of this form.
		 *
		 * Two steps in two different forms may have the same `id`.
		 * Two steps in two different versions of the same form may have the same `id`.
		 * However, two steps of the same versions of the same form cannot have the same `id`.
		 *
		 * Steps are sorted by their `id`, such that a step with a lesser `id` happens before a step with a greater `id`.
		 *
		 * Other than that, there are no rules on what `id` looks like.
		 * For example, it is allowed for the first step to have `id` `-1`, and for the second step to have `id` `15`.
		 * In practice, it is likely that most implementations will use incrementing numbers as `id`, but this is not
		 * guaranteed and should not be relied on.
		 */
		val id: Int,

		/**
		 * Human-readable name of this step.
		 */
		val name: String,

		/**
		 * The employees assigned to make a decision about submissions in this step.
		 *
		 * Any employees from the assigned department may make a decision.
		 * Administrators (no matter their department) can also make a decision.
		 */
		val reviewer: Department.Ref,

		/**
		 * Additional fields filled by the employee making the decision.
		 *
		 * Because of the nature of steps, it is unnecessary to add a 'validation' toggle.
		 * It is also unnecessary to add a 'reason' field.
		 * In simple cases, it is better not to add any fields (value of `null`).
		 *
		 * Examples of valid cases to use additional fields:
		 * - the step consists of deciding on a meeting date (the date should be a field),
		 * - the step consists of asking the user to provide a physical document to be scanned,
		 * - the step consists of transmitting the request to another tool (the field should be the request ID in that other tool, such that finding it is easy in the future).
		 */
		val field: Field?,
	)

	interface Service : Backbone<Ref, Failures.Get, Form>, IdentifierParser<Ref> {

		val versions: Version.Service

		/**
		 * Lists all forms.
		 *
		 * Forms are returned based on the current user's rights:
		 * - guests only see [public] forms,
		 * - employees see all forms.
		 */
		suspend fun list(includeClosed: Boolean = false): Outcome<Failures.List, List<Ref>>

		/**
		 * Creates a new form.
		 *
		 * Only administrators can create forms.
		 */
		suspend fun create(
			name: String,
			firstVersionTitle: String,
			field: Field,
			vararg step: Step,
		): Outcome<Failures.Create, Ref>

		// This is a compiler trick to make the vararg mandatory
		@Suppress("DeprecatedCallableAddReplaceWith")
		@Deprecated("It is not allowed to create a form with no review steps.", level = DeprecationLevel.ERROR)
		fun create(
			name: String,
			firstVersionTitle: String,
			field: Field,
		): Outcome<Failures.Create, Ref> = throw NotImplementedError("It is not allowed to create a form with no review steps.")
	}

	sealed interface Failures {
		sealed interface Get : Failures
		sealed interface List : Failures
		sealed interface Create : Failures
		sealed interface CreateVersion : Failures, Create
		sealed interface Edit : Failures

		data class NotFound(override val id: Ref) : StandardNotFound<Ref>,
			Get,
			Edit,
			CreateVersion

		object Unauthenticated : StandardUnauthenticated,
			Get,
			List,
			Create,
			CreateVersion,
			Edit

		object Unauthorized : StandardUnauthorized,
			List,
			Create,
			CreateVersion,
			Edit

		data class InvalidImport(
			val failures: Nel<Field.Failures.Compatibility>,
		) : Create,
			CreateVersion
	}
}
