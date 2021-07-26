package formulaide.api.data

import formulaide.api.data.FormSubmission.Companion.createSubmission
import formulaide.api.data.FormSubmission.ReadOnlyAnswer.Companion.asAnswer
import formulaide.api.fields.Field
import formulaide.api.fields.FormField
import formulaide.api.fields.ShallowFormField
import formulaide.api.fields.SimpleField.Message
import formulaide.api.types.Arity
import formulaide.api.types.Ref
import formulaide.api.types.Ref.Companion.SPECIAL_TOKEN_NEW
import formulaide.api.types.Ref.Companion.createRef
import formulaide.api.types.Ref.Companion.ids
import formulaide.api.types.Referencable
import formulaide.api.types.ReferenceId
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
 * - Starting at the top-level of the form, the first ID is the top-level's field ID ([ShallowFormField.id])
 * - If the field has a [maximum arity][Arity.max] greater than 1, an arbitrary integer
 * (of your choice) is used to represent the index of the element
 * (repeat until the maximum arity is 1).
 * - If the field is a [composite type][Composite], [Field.id] is added
 * (repeat from the previous step as long as necessary).
 *
 * When encountering a [Unions][Field.Union], you should:
 * - Answer the union (as if it were a simple field) by giving the ID of the selected field,
 * - Provide any required subfields as if the union was a composite object that had a single field.
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
 * @property root Which root of this [form] this submission corresponds to.
 * `null` means that the submission refers to the [main root][Form.mainFields], otherwise it corresponds to the referenced action's [root][Action.fields].
 * @property data A dictionary of the user's responses to the data requested by the [Form].
 * See [FormSubmission] for the format description.
 */
@Serializable
data class FormSubmission(
	override val id: ReferenceId,
	val form: Ref<Form>,
	val root: Ref<Action>? = null,
	val data: Map<String, String>,
) : Referencable {

	//region Check validity

	/**
	 * Checks the validity of this [FormSubmission].
	 *
	 * It is expected that the form is validated and loaded (see [Form.validate]).
	 *
	 * @throws IllegalStateException if the submission is invalid.
	 */
	fun checkValidity(form: Form) {
		this.form.load(form)
		println("\nChecking validity of $this…")

		val selectedRoot = when (root) {
			null -> form.mainFields
			else -> {
				root.loadFrom(form.actions, allowNotFound = false)
				root.obj.fields
			}
		}

		val topLevelAnswer = data
			.mapKeys { (key, _) ->
				key.split(":")
			}
			.toList()
			.asAnswer()

		for (field in selectedRoot.fields) {
			println("• Top-level field ${field.id}, ‘${field.name}’")
			checkField(field, topLevelAnswer)
		}
	}

	/**
	 * Checks that a specific [field] validates all its constraints.
	 */
	private fun checkField(
		field: FormField,
		answer: ReadOnlyAnswer,
	) {
		// Checking field.id
		val childAnswerOrNull = answer.components[field.id]

		// Checking field.arity
		val childrenAnswers = checkArity(field, childAnswerOrNull)

		// Checking field type for each answer
		for (childAnswer in childrenAnswers) {
			@Suppress("UNUSED_VARIABLE") // force exhaustive when
			val e = when (field) {
				is FormField.Simple -> checkSimple(field, childAnswer)
				is FormField.Union<*> -> checkUnion(field, childAnswer)
				is FormField.Composite -> checkComposite(field, childAnswer)
			}
		}
	}

	/**
	 * Checks that an [answer] corresponds to [field]'s [arity][Field.arity].
	 */
	private fun checkArity(
		field: FormField,
		answer: ReadOnlyAnswer?,
	): List<ReadOnlyAnswer> {
		val fieldArity =
			if (field is Field.Simple && field.simple == Message) Arity.forbidden()
			else field.arity

		val answers = when {
			answer == null -> emptyList()
			fieldArity.max > 1 -> answer.components.map { (_, value) -> value }
			else -> listOf(answer)
		}
		require(answers.size in fieldArity.range) { "${fieldErrorMessage(field)} a une arité de ${fieldArity}, mais ${answers.size} valeurs ont été données : $answer" }
		return answers
	}

	private fun checkComposite(
		field: FormField.Composite,
		answer: ReadOnlyAnswer,
	) {
		for (compositeField in field.fields) {
			println("• Field ${compositeField.id}, ’${compositeField.name}’")

			// Finding the FormField that corresponds to this CompoundDataField
			val formField = field.fields.find { it.id == compositeField.id }
			requireNotNull(formField) { "La donnée '${field.name}' ('${field.id}') a un champ ${compositeField.id}, mais le formulaire donné n'a pas de champ équivalent : ${field.fields.ids()}" }

			checkField(formField, answer)
		}
	}

	private fun checkUnion(
		field: FormField.Union<*>,
		answer: ReadOnlyAnswer,
	) {
		val selectedId = answer.value
			?: error("${fieldErrorMessage(field)} est une union, elle doit avoir une unique valeur ; trouvé le choix ${answer.value}")
		val selectedField = field.options.find { it.id == selectedId }
			?: error("${fieldErrorMessage(field)} est une union, mais le choix donné ne correspond à aucun des choix disponibles : $selectedId")

		val selectedChildren = checkArity(
			selectedField,
			answer.takeIf { it.components.isNotEmpty() }
				?.copy(value = null)
		)
		require(selectedChildren.size <= 1) { "${fieldErrorMessage(field)} est une union, elle ne peut pas avoir plus d'une valeur ; trouvé les clefs ${answer.components.map { it.key }}" }
		val selectedChild =
			selectedChildren.firstOrNull() // we can't lose data because the max size is 1

		if (selectedChild != null) {
			@Suppress("UNUSED_VARIABLE") // The variable is there to force the 'when' to be exhaustive
			val e = when (val formField: FormField = selectedField) {
				is FormField.Composite -> checkComposite(formField, selectedChild)
				is FormField.Union<*> -> checkUnion(formField, selectedChild)
				is FormField.Simple -> checkSimple(formField, selectedChild)
			}
		}
	}

	/**
	 * Checks that an [answer] corresponds to a given [field].
	 */
	private fun checkSimple(
		field: FormField.Simple,
		answer: ReadOnlyAnswer,
	) {
		field.simple.validate(answer.value)

		require(answer.components.isEmpty()) { "${fieldErrorMessage(field)} est de type SIMPLE, il ne peut pas avoir des sous-réponses ; trouvé ${answer.components}" }
	}

	private fun fieldErrorMessage(field: FormField) =
		"Le champ '${field.id}' (nommé '${field.name}')"

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
		override val components: Map<String, ReadOnlyAnswer>,
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
	) : Answer() {

		//region Data DSL

		private fun simpleValue(field: FormField.Simple, value: String?) {
			field.simple.validate(value)
			components += field.id to MutableAnswer(value)
		}

		fun text(field: FormField.Simple, value: String) = simpleValue(field, value)
		fun message(field: FormField.Simple) = simpleValue(field, null)
		fun integer(field: FormField.Simple, value: Long) =
			simpleValue(field, value.toString())

		fun decimal(field: FormField.Simple, value: Double) =
			simpleValue(field, value.toString())

		fun boolean(field: FormField.Simple, value: Boolean) =
			simpleValue(field, value.toString())

		private fun abstractList(
			field: Field,
			headValue: String?,
			block: MutableAnswer.() -> Unit,
		) {
			val list = MutableAnswer(headValue)
			list.block()
			components += field.id to list
		}

		fun item(id: Int, block: MutableAnswer.() -> Unit) {
			val list = MutableAnswer(null)
			list.block()
			components += id.toString() to list
		}

		fun <F : FormField> union(
			field: FormField.Union<F>,
			choice: F,
			block: MutableAnswer.() -> Unit,
		) = abstractList(field, choice.id, block)

		fun composite(field: FormField.Composite, block: MutableAnswer.() -> Unit) =
			abstractList(field, null, block)

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
		 *
		 * It is expected that the [Form] this is called on has already been [validated][Form.validate].
		 */
		fun Form.createSubmission(
			root: Action? = null,
			block: MutableAnswer.() -> Unit,
		): FormSubmission {
			val form = this
			val answer = MutableAnswer(null, mutableMapOf())

			answer.block()

			return FormSubmission(
				SPECIAL_TOKEN_NEW,
				form.createRef(),
				root?.createRef(),
				data = answer.flatten()
			).apply {
				checkValidity(form)
			}
		}

	}
}
