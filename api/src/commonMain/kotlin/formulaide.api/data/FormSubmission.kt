package formulaide.api.data

import formulaide.api.data.FormSubmission.Companion.createSubmission
import formulaide.api.data.FormSubmission.ReadOnlyAnswer.Companion.asAnswer
import formulaide.api.fields.*
import formulaide.api.fields.SimpleField.Message
import formulaide.api.types.Arity
import formulaide.api.types.Ref
import formulaide.api.types.Ref.Companion.createRef
import kotlinx.serialization.Serializable

@DslMarker
annotation class FormSubmissionDsl

/**
 * A submission of a [Form].
 *
 * Unlike [Composite], and [Form], this data point is not recursive,
 * in order to make it easier to use. Instead, this is simply a [dictionary][Map]
 * of each value given by the user.
 *
 * ### Format
 *
 * The [data] dictionary is composed of:
 * - A key, that identifies which field is being submitted,
 * - A value, which is the user's response.
 *
 * The value is a [String] corresponding to the field's [type][FormField].
 * If the field is optional (see [Arity]) or doesn't require an answer (eg. [Message]),
 * do not add the value if none is required (`null` will *not* be accepted).
 *
 * The key is formed by grouping different IDs, separated by the character 'colon' (`:`).
 * The key is built using the following steps (the order is important):
 * - Starting at the top-level of the form, the first ID is the top-level's field ID ([FormField.id])
 * - If the field has a [maximum arity][Arity.max] greater than 1, an arbitrary integer
 * (of your choice) is used to represent the index of the element
 * (repeat until the maximum arity is 1).
 * - If the field is a [composite type][Composite], [Field.id] is added
 * (repeat from the previous step as long as necessary).
 *
 * [Unions][Field.Union] are treated as compound objects that only have one field,
 * whose ID is the ID of the element of the enum that was selected.
 * The value corresponds to the value of the selected union element.
 *
 * ### Example
 *
 * In this example, all IDs are specifically chosen to be different if and only if
 * they refer to different objects, for readability reasons. This is not the case
 * in the real world, see the documentation of each ID. The example has been greatly simplified,
 * and is provided in YAML-like format for readability (this format is **not** accepted by the API,
 * this is only an illustration of the key-value system).
 *
 * Example data (real data is modeled by [Composite]):
 * ```yaml
 *   name: Identity
 *   id: 1
 *   fields:
 *     - Last name (id: 2, mandatory, TEXT)
 *     - First name (id: 3, mandatory, TEXT)
 *     - Phone number (id: 4, optional, NUMBER)
 *     - Family (id: 5, optional, COMPOUND Identity)
 * ```
 *
 * Example form (read forms are modeled by [Form]):
 * ```yaml
 *   name: Foo
 *   id: 6
 *   fields:
 *     - Requester (id: 7, mandatory, Identity)
 *       - Last name (id: 2, mandatory, TEXT)
 *       - First name (id: 3, mandatory, TEXT)
 *       - Phone number (id: 4, mandatory, NUMBER)
 *       - Family (id: 5, list, COMPOUND Identity)
 *         - Last name (id: 2, mandatory, TEXT)
 *         - First name (id: 3, mandatory, TEXT)
 *         - Phone number (id: 4, optional, NUMBER)
 *     - Preferred location (id: 9, mandatory, UNION)
 *       - By the sea (id: 10)
 *       - By the town hall (id: 11)
 *     - Comments (id: 12, optional, MESSAGE)
 * ```
 *
 * A valid submission for this form would be:
 * ```yaml
 * Example submission:
 *   form: 6
 *   data:
 *     "7:2": "My Last Name"
 *     "7:3": "My First Name"
 *     "7:4": "+33 1 23 45 67 89"
 *     "7:5:0:2": "My Brother's Last Name"
 *     "7:5:0:3": "My Brother's First Name"
 *     "7:5:1:2": "My Sister's Last Name"
 *     "7:5:1:3": "My Sister's First Name"
 *     "9:10": "Whatever can fit here, because the type is MESSAGE"
 * ```
 * Here, the user has a brother and sister, provided their own phone number but not their sibling's,
 * wants to live by the sea, and doesn't have any comments.
 *
 * @constructor Internal constructor to build this object. Use the factory method [Form.createSubmission].
 * @property form The [Form] this submission corresponds to.
 * @property data A dictionary of the user's responses to the data requested by the [Form].
 * See [FormSubmission] for the format description.
 */
@Serializable
data class FormSubmission(
	val form: Ref<Form>,
	val data: Map<String, String>,
) {

	//region Check validity

	fun checkValidity(form: Form, compounds: List<CompoundData>) {
		require(this.form == form.id) { "L'identifiant du formulaire donné à checkValidity est différent de celui correspondant à cette saisie" }
		println("\nChecking validity of $this…")

		val topLevelAnswer = data
			.mapKeys { (key, _) ->
				key.split(":")
			}
			.toList()
			.asAnswer()

		checkTopLevelFieldsValidity(form.fields, topLevelAnswer, compounds)
	}

	private fun checkTopLevelFieldsValidity(
		fields: List<FormField>,
		topLevelAnswer: ReadOnlyAnswer,
		compounds: List<CompoundData>
	) {
		for (field in fields) {
			println("• Top-level field ${field.id}, ‘${field.name}’")
			checkField(topLevelAnswer, field, null, field, compounds)
		}
	}

	private fun checkDeepFieldsValidity(
		fields: List<FormFieldComponent>,
		topLevelAnswer: ReadOnlyAnswer,
		compound: CompoundData,
		compounds: List<CompoundData>,
	) {
		for (compoundField in compound.fields) {
			println("• Field ${compoundField.id}, ’${compoundField.name}’")

			// Finding the FormField that corresponds to this CompoundDataField
			val formField = fields.find { it.id == compoundField.id }
			requireNotNull(formField) { "La donnée '${compound.name}' ('${compound.id}') a un champ ${compoundField.id}, mais le formulaire donné n'a pas de champ équivalent : ${fields.map { it.id }}" }

			checkField(topLevelAnswer, formField, compoundField, null, compounds)
		}
	}

	private fun checkField(
		topLevelAnswer: ReadOnlyAnswer,
		formField: AbstractFormField,
		compoundField: CompoundDataField? = null,
		topLevelFormField: FormField? = null,
		compounds: List<CompoundData>
	) {
		require((compoundField == null) xor (topLevelFormField == null)) { "Une seule des données entre 'compoundField' ($compoundField) et 'topLevelFormField' ($topLevelFormField) devraient être donnée" }
		val expectedData = compoundField?.data ?: topLevelFormField?.data!!

		// Checking field.id
		val answerOrNull = topLevelAnswer.components[formField.id.toString()]

		println("· Expected data: $expectedData\n· Given: $answerOrNull")

		// Checking field.arity
		val answers = checkFieldArity(answerOrNull, formField)

		// Checking field.data for each answer
		for (answer in answers) {
			@Suppress("UNUSED_VARIABLE")
			val e = when (expectedData) {
				is Data.Simple -> checkSimple(expectedData, answer, formField)
				is Data.Union -> checkUnion(expectedData, answer, formField, compounds)
				is Data.Compound -> checkCompound(
					expectedData,
					answer,
					formField,
					compounds
				)
			}
		}
	}

	private fun checkCompound(
		expectedType: Data.Compound,
		answer: ReadOnlyAnswer,
		field: AbstractFormField,
		compounds: List<CompoundData>
	) {
		val matchingCompound = compounds.find { it.id == expectedType.id }
		requireNotNull(matchingCompound) { "${fieldErrorMessage(field)} est de type COMPOUND et fait référence à '${expectedType.id}', mais la fonction de vérification n'y a pas accès (connues : ${compounds.map { it.id }})" }

		checkDeepFieldsValidity(field.components!!, answer, matchingCompound, compounds)
	}

	private fun fieldErrorMessage(field: AbstractFormField) =
		if (field is FormField) "Le champ '${field.id}' (nommé '${field.name}')"
		else "Le champ '${field.id}'"

	/**
	 * Checks that an [answer] corresponds to the [arity][Arity] of a given [AbstractFormField].
	 */
	private fun checkFieldArity(
		answer: ReadOnlyAnswer?,
		field: AbstractFormField,
	): List<ReadOnlyAnswer> {
		val answers = when {
			answer == null -> emptyList()
			field.arity.max > 1 -> answer.components.map { (_, value) -> value }
			else -> listOf(answer)
		}
		require(answers.size in field.arity.range) { "${fieldErrorMessage(field)} a une arité de ${field.arity}, mais ${answers.size} valeurs ont été données : $answer" }
		return answers
	}

	private fun checkUnion(
		expected: Data.Union,
		answer: ReadOnlyAnswer,
		field: AbstractFormField,
		compounds: List<CompoundData>
	) {
		require(answer.components.size == 1) { "${fieldErrorMessage(field)} est une union, elle ne peut avoir qu'une seule valeur ; trouvé les clefs ${answer.components.map { it.key }}" }
		val (unionAnswerId, unionAnswer) = answer.components.entries.first()

		val match = expected.elements.find { it.id.toString() == unionAnswerId }
		requireNotNull(match) { "${fieldErrorMessage(field)} est une union qui autorise les éléments ${expected.elements.map { it.id }}, dont la réponse donnée ne fait pas partie : ${answer.value}" }

		@Suppress("UNUSED_VARIABLE") // The variable is there to force the 'when' to be exhaustive
		val e = when (val expectedType = match.type) {
			is Data.Compound -> checkCompound(expectedType, unionAnswer, field, compounds)
			is Data.Union -> checkUnion(expectedType, unionAnswer, field, compounds)
			is Data.Simple -> checkSimple(expectedType, unionAnswer, field)
		}
	}

	private fun checkSimple(
		expected: Data.Simple,
		answer: ReadOnlyAnswer,
		field: AbstractFormField,
	) {
		val validated = expected.id.validate(answer.value)
		requireNotNull(validated) { "${fieldErrorMessage(field)} est de type ${expected.id}, mais la valeur donnée ne correspond pas : '${answer.value}'" }

		require(answer.components.isEmpty()) { "${fieldErrorMessage(field)} est de type SIMPLE, il ne peut pas avoir des sous-réponses ; trouvé ${answer.components}" }
	}

	//endregion
	//region Form answers

	/**
	 * Recursive tree that represents a [form submission][FormSubmission]'s contents.
	 *
	 * Each node has a [value] and multiple [sub-nodes][components].
	 */
	abstract class Answer {

		/**
		 * The value of the current node.
		 */
		internal abstract val value: String?

		/**
		 * The sub-nodes of this [Answer].
		 * The key corresponds to the ID of that depth (see [FormSubmission]), the value corresponds to the answer of that depth.
		 */
		internal abstract val components: Map<String, Answer>
	}

	private data class ReadOnlyAnswer(
		override val value: String?,
		override val components: Map<String, ReadOnlyAnswer>
	) : Answer() {

		companion object {
			fun List<Pair<List<String>, String>>.asAnswer(): ReadOnlyAnswer {

				val elements = this
					.sortedBy { (ids, _) -> ids.size }
					.groupBy { (ids, _) -> ids.getOrNull(0) }

				val headList = elements[null] ?: listOf(emptyList<String>() to null)
				require(headList.size == 1) { "Il ne peut pas y avoir plusieurs entrées avec le même identifiant : ${headList.map { it.first }}" }
				val head = headList[0].second

				return elements
					.filterKeys { it != null }
					.mapKeys { (ids, _) -> ids!! }
					.mapValues { (_, values) ->
						values.map { (ids, value) ->
							ids.subList(1, ids.size) to value
						}.asAnswer()
					}
					.toList()
					.fold(ReadOnlyAnswer(head, emptyMap())) { acc, (id, value) ->
						ReadOnlyAnswer(acc.value, acc.components + mapOf(id to value))
					}
			}
		}
	}

	/**
	 * Mutable implementation of [Answer], used as a DSL in [Form.createSubmission].
	 */
	@FormSubmissionDsl
	data class MutableAnswer internal constructor(
		override val value: String?,
		override val components: MutableMap<String, MutableAnswer> = HashMap(),
		val composites: List<Composite>,
	) : Answer() {

		//region Data DSL

		private fun simpleValue(field: Field.Simple, value: String?) {
			field.simple.validate(value)
			components += field.id to MutableAnswer(value, composites = composites)
		}

		fun text(field: FormRoot.SimpleFormField, value: String) = simpleValue(field, value)
		fun message(field: FormRoot.SimpleFormField) = simpleValue(field, null)
		fun integer(field: FormRoot.SimpleFormField, value: Long) = simpleValue(field, value.toString())
		fun decimal(field: FormRoot.SimpleFormField, value: Double) = simpleValue(field, value.toString())
		fun boolean(field: FormRoot.SimpleFormField, value: Boolean) = simpleValue(field, value.toString())

		private fun abstractList(field: Field, headValue: String?, block: MutableAnswer.() -> Unit) {
			val list = MutableAnswer(headValue, composites = composites)
			list.block()
			components += field.id to list
		}

		fun item(id: Int, block: MutableAnswer.() -> Unit) {
			val list = MutableAnswer(null, composites = composites)
			list.block()
			components += id.toString() to list
		}

		fun <F : Field> union(field: FormRoot.UnionFormField<F>, choice: F, block: MutableAnswer.() -> Unit) = abstractList(field, choice.id.toString(), block)

		fun compound(field: FormRoot.CompositeFormField, block: MutableAnswer.() -> Unit) = abstractList(field, null, block)

		//endregion

		private fun flattenIntermediary(): Map<out String?, String> {
			val flatComponents = components
				.mapValues { (_, answer) -> answer.flattenIntermediary() }
				.flatMap { (id, components) ->
					components.map { (k, v) -> if (k != null) ("$id:$k") to v else id to v }
				}
				.toMap()

			return if (value != null)
				flatComponents + mapOf<String?, String>(null to value)
			else
				flatComponents
		}

		internal fun flatten(): Map<String, String> {
			val flattened = flattenIntermediary()

			@Suppress("UNCHECKED_CAST")
			return flattened.filterKeys { it != null } as Map<String, String>
		}
	}

	//endregion

	companion object {

		/**
		 * DSL builder to create a valid [FormSubmission].
		 */
		fun Form.createSubmission(
			composites: List<Composite>,
			block: MutableAnswer.() -> Unit
		): FormSubmission {
			val form = this
			val answer = MutableAnswer(null, mutableMapOf(), composites)

			answer.block()

			return FormSubmission(
				form.createRef(),
				data = answer.flatten()
			).apply {
				checkValidity(form, composites)
			}
		}

	}
}
