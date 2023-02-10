package opensavvy.formulaide.core

import kotlinx.datetime.Instant
import opensavvy.backbone.Backbone
import opensavvy.formulaide.core.Form.*
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

	val versionsSorted by lazy(LazyThreadSafetyMode.PUBLICATION) { versions.sortedBy { it.version } }

	data class Ref(
		val id: String,
		override val backbone: Service,
	) : opensavvy.backbone.Ref<Form> {

		/**
		 * Renames this form.
		 *
		 * @see Form.name
		 * @see Service.edit
		 */
		suspend fun rename(name: String) = backbone.edit(this, name = name)

		/**
		 * Opens this form.
		 *
		 * @see Form.open
		 * @see Service.edit
		 */
		suspend fun open() = backbone.edit(this, open = true)

		/**
		 * Closes this form.
		 *
		 * @see Form.open
		 * @see Service.edit
		 */
		suspend fun close() = backbone.edit(this, open = false)

		/**
		 * Makes this form public.
		 *
		 * @see Form.open
		 * @see Service.edit
		 */
		suspend fun publicize() = backbone.edit(this, public = true)

		/**
		 * Makes this form private.
		 *
		 * @see Form.open
		 * @see Service.edit
		 */
		suspend fun privatize() = backbone.edit(this, public = false)

		/**
		 * Creates a new [version] of this template.
		 *
		 * @see Template.versions
		 * @see Service.createVersion
		 */
		suspend fun createVersion(version: Version) = backbone.createVersion(this, version)

		/**
		 * Creates a new version of this template.
		 *
		 * @see Template.versions
		 * @see Service.createVersion
		 */
		suspend fun createVersion(title: String, field: Field, vararg step: Step) =
			createVersion(Version(Instant.Companion.DISTANT_PAST, title, field, step.asList()))

		// This is a compiler trick to make the vararg mandatory
		@Suppress("DeprecatedCallableAddReplaceWith", "UNUSED_PARAMETER")
		@Deprecated("It is not allowed to create a form with no review steps.", level = DeprecationLevel.ERROR)
		fun createVersion(title: String, field: Field): Outcome<Version.Ref> =
			throw NotImplementedError("It is not allowed to create a form with no review steps.")

		override fun toString() = "Formulaire $id"
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

		data class Ref(
			val form: Form.Ref,
			val version: Instant,
			override val backbone: Service,
		) : opensavvy.backbone.Ref<Version> {

			override fun toString() = "$form $version"
		}

		interface Service : Backbone<Version>
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

	interface Service : Backbone<Form> {

		val versions: Version.Service

		/**
		 * Lists all forms.
		 *
		 * Forms are returned based on the current user's rights:
		 * - guests only see [public] forms,
		 * - employees see all forms.
		 */
		suspend fun list(includeClosed: Boolean = false): Outcome<List<Ref>>

		/**
		 * Creates a new form.
		 *
		 * Only administrators can create forms.
		 */
		suspend fun create(name: String, firstVersion: Version): Outcome<Ref>

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
		) = create(name, Version(Instant.DISTANT_PAST, firstVersionTitle, field, step.asList()))

		// This is a compiler trick to make the vararg mandatory
		@Suppress("DeprecatedCallableAddReplaceWith")
		@Deprecated("It is not allowed to create a form with no review steps.", level = DeprecationLevel.ERROR)
		fun create(
			name: String,
			firstVersionTitle: String,
			field: Field,
		): Outcome<Ref> = throw NotImplementedError("It is not allowed to create a form with no review steps.")

		/**
		 * Creates a new version of a given form.
		 *
		 * Only administrators can create new versions.
		 */
		suspend fun createVersion(form: Ref, version: Version): Outcome<Version.Ref>

		/**
		 * Creates a new version of a given form.
		 *
		 * Only administrators can create new versions.
		 */
		suspend fun createVersion(form: Ref, title: String, field: Field, vararg step: Step) =
			createVersion(form, Version(Instant.DISTANT_PAST, title, field, step.asList()))

		// This is a compiler trick to make the vararg mandatory
		@Suppress("DeprecatedCallableAddReplaceWith")
		@Deprecated("It is not allowed to create a form with no review steps.", level = DeprecationLevel.ERROR)
		fun createVersion(form: Ref, title: String, field: Field): Outcome<Ref> =
			throw NotImplementedError("It is not allowed to create a form with no review steps.")

		/**
		 * Edits a form.
		 *
		 * Only administrators can edit forms.
		 */
		suspend fun edit(
			form: Ref,
			name: String? = null,
			open: Boolean? = null,
			public: Boolean? = null,
		): Outcome<Unit>
	}
}
