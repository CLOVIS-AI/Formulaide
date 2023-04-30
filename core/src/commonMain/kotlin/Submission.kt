package opensavvy.formulaide.core

import arrow.core.*
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import opensavvy.backbone.Backbone
import opensavvy.formulaide.core.Submission.Parsed
import opensavvy.formulaide.core.Submission.ParsingFailure.InvalidValue
import opensavvy.state.arrow.toEither
import opensavvy.state.coroutines.now
import opensavvy.state.failure.CustomFailure
import opensavvy.state.failure.Failure
import opensavvy.state.failure.Unauthenticated
import opensavvy.state.failure.Unauthorized

private typealias CanFail = Raise<NonEmptyList<Submission.ParsingFailure>>
private typealias MutableParsedData = MutableMap<Field.Id, Any>

/**
 * Answers to a [Field].
 *
 * Unlike [Field], [Submission] is not a recursive data structure.
 * It stores which [form] and [step][formStep] it relates to, as well as the answers in a flat [data structure][data].
 *
 * The [data] can be [parsed] into the [Parsed] class.
 *
 * Submissions are created as part of a [Record].
 */
data class Submission(
	/**
	 * The [Form.Version] this submission was created against.
	 *
	 * The [Field] instance this submission relates to can be found using [Form.Version.findFieldForStep] and [formStep].
	 */
	val form: Form.Version.Ref,

	/**
	 * The ID of the step in [form] this submission relates to.
	 *
	 * For more information on the allowed values, see [Form.Version.findFieldForStep].
	 */
	val formStep: Int?,

	/**
	 * A dictionary of the user's answers to the field identified by [form] and [formStep].
	 *
	 * ### Structure
	 *
	 * Each field is given an [identifier][Field.Id] by traversing from the root to itself.
	 * At each recursion step, the [field's index][Field.indexedFields] is appended to the identifier of its parent,
	 * separated by a colon (`:`).
	 *
	 * ### Labels
	 *
	 * [Labels][Field.Label] do not require any user input.
	 * They do not appear in submissions.
	 *
	 * ### Inputs
	 *
	 * Since [inputs][Field.Input] never store more than a single value, they are the simplest to represent.
	 * They are simply represented by an association between their identifier and their value.
	 *
	 * For example, if a textual input has the identifier `0:2:1` and the user answered `"test"`, it is stored
	 * as the association:
	 * ```
	 * 0:2:1 -> "test"
	 * ```
	 *
	 * In the case of file uploads, the value is the identifier of the uploaded file.
	 * For more information on the different input cases, see [Input].
	 *
	 * ### Choices
	 *
	 * [Choices][Field.Choice] allow the user to select between multiple options.
	 * To clarify which option the user has selected, the ID of the choice itself is associated with the relative ID
	 * of the selected field.
	 *
	 * As an example, let's take the following choice:
	 * ```
	 * - Select (Choice, 1:2)
	 *   - First option (Label, 1:2:3)
	 *   - Second option (Input text, 1:2:4)
	 * ```
	 *
	 * If the user wants to select the first option, they do not need to fill in any more information (they selected
	 * a label):
	 * ```
	 * 1:2   -> "3"
	 * ```
	 * However, if they selected the second option, which is a textual input, they must also submit a value for it:
	 * ```
	 * 1:2   -> "4"
	 * 1:2:4 -> "answer to the second option"
	 * ```
	 *
	 * ### Groups
	 *
	 * [Groups][Field.Group] group multiple fields into a single cohesive unit.
	 * All fields of a group must be filled in, or the entire group must be missing.
	 * To mark this decision, the group's ID must be present if the entire group is expected to be present.
	 * It can be associated to any value, but the empty string is preferred by convention.
	 *
	 * As an example, let's take the following group:
	 * ```
	 * - Example (Group, 1:2)
	 *   - First field (Label, 1:2:3)
	 *   - Second field (Input text, 1:2:4)
	 * ```
	 *
	 * If the group is filled in, a valid submission would look like:
	 * ```
	 * 1:2   -> "" // group presence marker
	 * 1:2:4 -> "answer to the second field"
	 * ```
	 * All subfields must be filled in according to their rules (e.g. inputs must have a value, labels must be absent…).
	 *
	 * ### Arity
	 *
	 * [Arity][Field.Arity] allows the user to fill in the same field zero or more times.
	 * It allows to encode whether a field is mandatory or optional.
	 * It behaves as if it had multiple subfields, identified `0` to `n`, following these rules:
	 * - fields identified `0` to the minimum of [Field.Arity.allowed] are mandatory,
	 * - fields identified from the minimum of [Field.Arity.allowed] to its maximum are optional.
	 *
	 * As an example, let's take the following arity:
	 * ```
	 * - Children (arity 1..3, 1:2)
	 *   - Child (Input text, 1:2:[0..2])
	 * ```
	 * Since the minimum arity is `1`, at least one answer is necessary. The minimal submission is therefore:
	 * ```
	 * 1:2:0 -> "first child"
	 * ```
	 * Since the maximum arity is `3`, at most 3 answers may be provided. The maximal submission is therefore:
	 * ```
	 * 1:2:0 -> "first child"
	 * 1:2:1 -> "second child"
	 * 1:2:2 -> "third child"
	 * ```
	 *
	 * This allows to create multiple patterns.
	 *
	 * #### Mandatory field
	 *
	 * By creating an arity with a minimum of `1` and a maximum of `1`, the encapsulated field is marked
	 * as mandatory. In practice, this is useless because fields are mandatory by default, however it may still happen
	 * for compatibility reasons (the field was previously optional, and the arity was kept to keep the same field identifier).
	 *
	 * #### Optional field
	 *
	 * By creating an arity with a minimum of `0` and a maximum of `1`, the encapsulated field is marked as optional.
	 * If the user chooses not to fill in the field, it should be entirely missing from the submission (e.g. the
	 * group presence marker should not exist).
	 *
	 * #### Optional list
	 *
	 * By creating an arity with a minimum of `0` and a maximum greater than `1`, the encapsulated field may be answered
	 * multiple times, but may also be entirely absent.
	 *
	 * #### Mandatory list
	 *
	 * By creating an arity with a minimum of `1` and a maximum greater than `1`, the encapsulated field may be answered
	 * multiple times, but at least once.
	 */
	val data: Map<Field.Id, String>,
) {

	private suspend fun submittedForField() = either {
		form.request()
			.now()
			.toEither()
			.mapLeft { ParsingFailure.UnavailableForm(it) }
			.bind()
			.findFieldForStep(formStep)
	}

	// region Verify and parse

	private lateinit var parsed: Parsed

	/**
	 * Parses and validates this [Submission].
	 *
	 * Parsing checks the [data] structure and extracts all valid information into the [Parsed] class.
	 *
	 * The results of this function are cached, such that subsequent calls are free.
	 */
	suspend fun parse(files: File.Service): EitherNel<ParsingFailure, Parsed> = either {
		if (::parsed.isInitialized)
			return@either parsed

		val field = submittedForField()
			.mapLeft { it.nel() }
			.bind()
		val parsedData = HashMap<Field.Id, Any>()
			.also { parseAny(Field.Id.root, field, mandatory = true, it, files) }

		Parsed(this@Submission, parsedData)
			.also { parsed = it }
	}

	private suspend fun CanFail.parseAny(
		id: Field.Id,
		field: Field,
		mandatory: Boolean,
		parsedData: MutableParsedData,
		files: File.Service,
	) {
		when (field) {
			is Field.Arity -> parseArity(id, field, mandatory, parsedData, files)
			is Field.Choice -> parseChoice(id, field, mandatory, parsedData, files)
			is Field.Group -> parseGroup(id, field, mandatory, parsedData, files)
			is Field.Input -> parseInput(id, field, mandatory, parsedData, files)
			is Field.Label -> {} // There is nothing to do, labels cannot have values
		}
	}

	private suspend fun CanFail.parseInput(
		id: Field.Id,
		field: Field.Input,
		mandatory: Boolean,
		parsedData: MutableParsedData,
		files: File.Service,
	) {
		val answer = data[id]

		if (!mandatory && answer == null) return

		ensure(answer != null) { InvalidValue.Mandatory(id).nel() }
		parsedData[id] = field.input.parse(answer, files)
			.toEither()
			.mapLeft { InvalidValue.InputParsingFailure(id, it).nel() }
			.bind()
	}

	@Suppress("FoldInitializerAndIfToElvis") // makes it harder to read in this case
	private suspend fun CanFail.parseChoice(
		id: Field.Id,
		field: Field.Choice,
		mandatory: Boolean,
		parsedData: MutableParsedData,
		files: File.Service,
	) {
		val selectedAsString = data[id]

		if (selectedAsString == null) {
			if (mandatory) raise(InvalidValue.Mandatory(id).nel())
			else return
		}

		val selected = selectedAsString.toIntOrNull()
		ensureNotNull(selected) { InvalidValue.SelectedChoiceMatchesNoOptions(id, selectedAsString, field.indexedFields.keys).nel() }

		val selectedField = field.indexedFields[selected]
		ensureNotNull(selectedField) { InvalidValue.SelectedChoiceMatchesNoOptions(id, selectedAsString, field.indexedFields.keys).nel() }

		parsedData[id] = selected
		parseAny(id + selected, selectedField, mandatory = true, parsedData, files)
	}

	private suspend fun CanFail.parseGroup(
		id: Field.Id,
		field: Field.Group,
		mandatory: Boolean,
		parsedData: MutableParsedData,
		files: File.Service,
	) {
		val present = data[id] != null

		if (!mandatory && !present)
			return

		ensure(present) { InvalidValue.MissingGroupMarker(id).nel() }

		field.indexedFields.mapOrAccumulate { (childId, child) ->
			parseAny(id + childId, child, mandatory = true, parsedData, files)
		}.mapLeft { it.flatMap(::identity) }
			.bind()
	}

	private suspend fun CanFail.parseArity(
		id: Field.Id,
		field: Field.Arity,
		mandatory: Boolean,
		parsedData: MutableParsedData,
		files: File.Service,
	) {
		(0 until field.allowed.last.toInt()).mapOrAccumulate {
			// The first allowed.first elements are as mandatory as the parent field
			// The next (allowed.last - allowed.first) elements are always optional
			val childMandatory =
				if (it in 0 until field.allowed.first.toInt()) mandatory
				else false

			parseAny(id + it, field.child, childMandatory, parsedData, files)
		}.mapLeft { it.flatMap(::identity) }
			.bind()
	}

	class Parsed(
		val submission: Submission,
		private val data: Map<Field.Id, Any>,
	) {

		operator fun get(id: Field.Id) = data[id]
	}

	sealed interface ParsingFailure {
		data class UnavailableForm(
			val failure: Form.Version.Failures.Get,
		) : ParsingFailure

		sealed class InvalidValue : ParsingFailure {
			abstract val field: Field.Id

			data class Mandatory(
				override val field: Field.Id,
			) : InvalidValue()

			data class InputParsingFailure(
				override val field: Field.Id,
				val failure: Input.Failures.Parsing,
			) : InvalidValue()

			data class SelectedChoiceMatchesNoOptions(
				override val field: Field.Id,
				val selected: String,
				val valid: Collection<Int>,
			) : InvalidValue()

			data class MissingGroupMarker(
				override val field: Field.Id,
			) : InvalidValue()
		}
	}

	// endregion

	override fun toString() = buildString {
		append("Saisie pour $form")

		if (formStep == null) append(" (saisie initiale)")
		else append(" (étape $formStep)")

		if (data.isEmpty()) append(" Vide")
		else {
			appendLine()

			val lines = data.map { (id, value) -> id.toString() to value }
			val longest = lines.maxOf { (id, _) -> id.length }

			for ((id, answer) in lines) {
				val displayedId = id.takeIf { it.isNotBlank() } ?: "root"
				appendLine("    ${displayedId.padEnd(longest)}    $answer")
			}
		}
	}

	interface Ref : opensavvy.backbone.Ref<Failures.Get, Submission>

	interface Service : Backbone<Ref, Failures.Get, Submission>

	interface Failures : Failure {
		sealed interface Get : Failures

		class NotFound(val ref: Ref) : CustomFailure(opensavvy.state.failure.NotFound(ref)),
			Get

		object Unauthenticated : CustomFailure(Unauthenticated()),
			Get

		object Unauthorized : CustomFailure(Unauthorized()),
			Get
	}

	companion object {

		fun Array<out Pair<String, String>>.toSubmissionData() = associateBy(
			keySelector = { Field.Id.fromString(it.first) },
			valueTransform = { it.second },
		)

	}
}
