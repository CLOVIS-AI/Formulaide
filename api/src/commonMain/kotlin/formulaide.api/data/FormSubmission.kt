package formulaide.api.data

import formulaide.api.data.FormSubmission.Companion.createSubmission
import formulaide.api.data.FormSubmission.ReadOnlyAnswer.Companion.asAnswer
import formulaide.api.dsl.formRoot
import formulaide.api.fields.Field
import formulaide.api.fields.FormField
import formulaide.api.fields.ShallowFormField
import formulaide.api.fields.SimpleField.Message
import formulaide.api.types.Arity
import formulaide.api.types.Ref
import formulaide.api.types.Ref.Companion.SPECIAL_TOKEN_NEW
import formulaide.api.types.Ref.Companion.createRef
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

	//region Parsing

	fun parse(form: Form): ParsedSubmission {
		this.form.load(form)
		println("\nParsing $this…")

		val selectedRoot = when (root) {
			null -> form.mainFields
			else -> {
				root.loadFrom(form.actions, allowNotFound = false)
				root.obj.fields ?: formRoot {}
			}
		}

		val topLevelAnswer = data
			.mapKeys { (key, _) ->
				key.split(":")
			}
			.toList()
			.asAnswer()

		return selectedRoot.fields.mapNotNull { field ->
			parseFieldWithArity(emptyList(), field, topLevelAnswer.components[field.id])
		}.let { ParsedSubmission(it) }
	}

	private fun <F : FormField> parseFieldWithArity(
		parent: List<String>,
		field: F,
		answer: ReadOnlyAnswer?,
	): ParsedField<F>? {
		val fieldArity =
			if (field is Field.Simple && field.simple == Message) Arity.forbidden()
			else field.arity

		return when {
			fieldArity.max > 1 -> {
				val answers = answer
					?.components
					?.map { (_, value) -> value }
					?: emptyList()
				println("$parent ${field.id} -> list of ${answers.size} elements ; the keys of the children will be incorrect in the logs")

				require(answers.size in fieldArity.range) { "${fieldErrorMessage(field)} a une arité de ${fieldArity}, mais ${answers.size} valeurs ont été données : $answer" }

				val parsedAnswers = answers.mapNotNull {
					parseField(parent + field.id,
					           field,
					           it,
					           overrideArity = Arity.optional())
				}
				ParsedList(field, parsedAnswers)
					.apply {
						parsedAnswers.forEachIndexed { index, it ->
							it.parent = this; it.key = index.toString()
						}
					}
			}
			else -> {
				parseField(parent, field, answer)
			}
		}
	}

	private fun <F : FormField> parseField(
		parent: List<String>,
		field: F,
		answer: ReadOnlyAnswer?,
		overrideArity: Arity? = null,
	): ParsedField<F>? {
		return when (field) {
			is FormField.Simple -> parseSimple(parent, field, answer, overrideArity)
			is FormField.Union<*> -> parseUnion(parent, field, answer, overrideArity)
			is FormField.Composite -> parseComposite(parent, field, answer)
			else -> error("Trouvé un champ de type impossible : $field")
		}
	}

	private fun <C : FormField.Composite> parseComposite(
		parent: List<String>,
		field: C,
		answer: ReadOnlyAnswer?,
	): ParsedComposite<C> {
		println("$parent ${field.id} -> composite $field")

		val children = field.fields.mapNotNull { formField ->
			parseFieldWithArity(parent + field.id, formField, answer?.components?.get(formField.id))
		}

		return ParsedComposite(field, children)
			.apply { children.forEach { it.parent = this } }
	}

	private fun <U : FormField.Union<C>, C : FormField> parseUnion(
		parent: List<String>,
		field: U,
		answer: ReadOnlyAnswer?,
		overrideArity: Arity? = null,
	): ParsedUnion<U, C>? {
		println("$parent ${field.id} -> union $field")
		val arity = overrideArity ?: field.arity

		val selectedId = answer?.value
		if (arity.min == 0 && selectedId == null) return null
		requireNotNull(selectedId) { "${fieldErrorMessage(field)} est une union, elle doit avoir une unique valeur ; trouvé le choix ${answer?.value}" }

		val selectedField = field.options.find { it.id == selectedId }
			?: error("${fieldErrorMessage(field)} est une union, mais le choix donné ne correspond à aucun des choix disponibles : $selectedId")

		val child = answer.components[selectedId]
		val parsedChild = parseFieldWithArity(parent + field.id, selectedField, child)

		return when {
			selectedField is FormField.Simple && selectedField.simple is Message -> {
				require(parsedChild == null) { "${fieldErrorMessage(field)} est une union, l'utilisateur a choisi un champ de type $Message, il ne peut donc pas fournir une donnée : $child" }
				ParsedUnion(field, selectedField, emptyList())
			}
			parsedChild == null -> null
			else -> ParsedUnion(field, selectedField, listOf(parsedChild))
				.apply { parsedChild.parent = this }
		}
	}

	private fun <S : FormField.Simple> parseSimple(
		parent: List<String>,
		field: S,
		answer: ReadOnlyAnswer?,
		overrideArity: Arity? = null,
	): ParsedSimple<S>? {
		val arity = overrideArity ?: field.arity
		return when {
			arity.min == 0 && (answer == null || answer.value.isNullOrBlank()) -> {
				println("$parent ${field.id} -> simple, was not filled in")

				field.simple.defaultValue?.let {
					println("$parent ${field.id} -> however, it has a default value: '$it'")
					ParsedSimple(field, it)
				} // else null
			}
			field.simple is Message -> {
				println("$parent ${field.id} -> simple $Message")
				require(answer == null) { "${fieldErrorMessage(field)} est un champ de type $Message, il ne peut donc pas avoir de valeur : $answer" }
				null
			}
			else -> {
				requireNotNull(answer) { "${fieldErrorMessage(field)} est un champ obligatoire (${field.arity}), mais aucune réponse n'a été trouvée" }
				println("$parent ${field.id} -> simple ${answer.value}")

				require(!answer.value.isNullOrBlank()) { "${fieldErrorMessage(field)} est un champ obligatoire (${field.arity}), mais la réponse donnée est vide : '${answer.value}'" }
				field.simple.parse(answer.value)
				require(answer.components.isEmpty()) { "${fieldErrorMessage(field)} est de type SIMPLE, il ne peut pas avoir des sous-réponses ; trouvé ${answer.components}" }

				ParsedSimple(field, answer.value)
			}
		}
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
		private var listIndex: Int? = null,
	) : Answer() {

		//region Data DSL

		private fun add(id: String, value: MutableAnswer) {
			if (listIndex == null)
				components += id to value
			else {
				components += (listIndex!!).toString() to value
				listIndex = listIndex!! + 1
			}
		}

		private fun simpleValue(field: FormField.Simple, value: String?) {
			field.simple.parse(value)
			add(field.id, MutableAnswer(value))
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
			add(field.id, list)
		}

		fun list(field: FormField, block: MutableAnswer.() -> Unit) {
			val list = MutableAnswer(null, listIndex = 0)
			list.block()
			add(field.id, list)
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
				parse(form)
			}
		}

	}
}
