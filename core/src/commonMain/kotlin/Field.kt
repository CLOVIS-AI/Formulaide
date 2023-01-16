package opensavvy.formulaide.core

/**
 * A field in a form or template.
 *
 * Fields are recursive data structures:
 * - [labels][Label] are simple text displayed to the user,
 * - [inputs][Input] are regular inputs which request some information from the user,
 * - [choices][Choice] allow the user to select one of multiple options,
 * - [groups][Group] join multiple fields into a cohesive unit,
 * - [lists][List] mark fields as optional, or allow answering a single field multiple times.
 */
sealed class Field {

	/**
	 * Short explanation of the role of this field, displayed to the user.
	 *
	 * Should not be blank.
	 */
	abstract val label: String

	/**
	 * Children of this field.
	 *
	 * Fields should be displayed to the user in the same order as they appear in this dictionary (not necessarily the order of the keys).
	 */
	abstract val indexedFields: Map<Int, Field>

	/**
	 * Children of this field.
	 *
	 * Fields should be displayed to the user in the same order as they appear in this dictionary (not necessarily the order of the keys).
	 */
	val fields: Sequence<Field>
		get() = indexedFields
			.values
			.asSequence()

	/**
	 * Origin of this field.
	 *
	 * If this field was imported from a [Template], this stores the version of that template.
	 *
	 * If this field was not imported from a template, it is `null`.
	 */
	abstract val importedFrom: Template.Version.Ref?

	/**
	 * Finds a field recursively by its [id].
	 *
	 * Returns `null` if the field couldn't be found.
	 *
	 * @see Field.Id
	 */
	operator fun get(id: Id): Field? =
		if (id.isRoot) this
		else indexedFields[id.head]?.get(id.tail)

	/**
	 * Finds a field recursively by its [id].
	 *
	 * Returns `null` if the field couldn't be found.
	 *
	 * @see Field.Id
	 */
	operator fun get(vararg id: Int) = get(Id(*id))

	protected fun verify() {
		require(label.isNotBlank()) { "Le libellé d'un champ ne peut pas être vide : '$label'" }
	}

	//region Types

	/**
	 * A field with no input.
	 *
	 * Visually, the [label] is displayed but no input is requested of the user.
	 *
	 * This type is used to embed non-interactive information into a form (legal text…).
	 *
	 * Labels never have children.
	 */
	data class Label(
		override val label: String,
		override val importedFrom: Template.Version.Ref?,
	) : Field() {
		override val indexedFields: Map<Int, Field>
			get() = emptyMap()

		init {
			verify()
		}

		override fun toString() = "Label($label)"
	}

	/**
	 * A regular input field.
	 *
	 * This field doesn't have children, but it does directly request some [input] from the user.
	 */
	data class Input(
		override val label: String,
		val input: opensavvy.formulaide.core.Input,
		override val importedFrom: Template.Version.Ref?,
	) : Field() {
		override val indexedFields: Map<Int, Field>
			get() = emptyMap()

		init {
			verify()
		}

		override fun toString() = "Input($label, $input)"
	}

	/**
	 * Multiple choices among which the user must select one.
	 */
	data class Choice(
		override val label: String,
		override val indexedFields: Map<Int, Field>,
		override val importedFrom: Template.Version.Ref?,
	) : Field() {

		init {
			verify()
		}

		override fun toString() = "Choice($label, $indexedFields)"
	}

	/**
	 * Multiple fields that the user must fill in.
	 */
	data class Group(
		override val label: String,
		override val indexedFields: Map<Int, Field>,
		override val importedFrom: Template.Version.Ref?,
	) : Field() {

		init {
			verify()
		}

		override fun toString() = "Group($label, $indexedFields)"
	}

	/**
	 * Controls whether fields are mandatory or optional.
	 */
	data class Arity(
		override val label: String,
		val child: Field,
		val allowed: UIntRange,
		override val importedFrom: Template.Version.Ref?,
	) : Field() {

		init {
			verify()

			// Answering the same field more than 100 times is probably a mistake
			require(allowed.last < 100u) { "il n'est pas possible de répondre à un même champ plus de 100 fois, vous avez demandé ${allowed.last}" }
		}

		override val indexedFields: Map<Int, Field>
			get() = List(allowed.last.toInt()) { it to child }.toMap()

		override fun toString() = "Arity($label, allowed=$allowed, $child)"
	}

	//endregion
	//region Specific

	/**
	 * Identifier for a specific [Field] in a given field hierarchy.
	 *
	 * Children fields of specific fields are identified by an integer (see [indexedFields]).
	 * The [Id] class represents the result of walking down the hierarchy to find the field we are mentioning.
	 *
	 * For example, in this field hierarchy:
	 * ```
	 * identity
	 *   1. first name
	 *   2. last name(s)
	 *     1. last name
	 *     2. last name
	 * ```
	 * we can identify each field using its ID:
	 * - `""`: identity,
	 * - `"1"`: first name
	 * - `"2"`: last name(s)
	 * - `"2:1"`: last name
	 * - `"2:2"`: last name
	 *
	 * Identifiers are used to find a field deeply (see [Field.get]).
	 */
	data class Id(private val parts: List<Int>) {

		constructor(vararg id: Int) : this(id.asList())

		operator fun plus(other: Id) = Id(parts + other.parts)

		operator fun plus(other: Int) = Id(parts + other)

		/**
		 * `true` if this identifier refers to the field hierarchy [root].
		 */
		val isRoot: Boolean
			get() = parts.isEmpty()

		/**
		 * The first element of this [Id].
		 *
		 * `null` if this identifier is the [root][isRoot].
		 */
		val headOrNull: Int?
			get() = parts.firstOrNull()

		/**
		 * The first element of this [Id].
		 *
		 * Throws [NoSuchElementException] if this identifier is the [root][isRoot].
		 */
		val head: Int
			get() = parts.first()

		/**
		 * All elements of this [Id] except the [headOrNull].
		 *
		 * The tail is useful to transpose an identifier for a field to an identifier in one of its children.
		 * For example, if we have the following fields:
		 * ```
		 * identity
		 *   1. address
		 *     2. city
		 *       3. name
		 * ```
		 * From the field 'identity', we can refer to 'name' using `"1:2:3"`.
		 * The tail of `"1:2:3"` is `"2:3"`, which is the way to refer to 'name' from the field 'address', the direct
		 * child of 'identity'.
		 *
		 * The tail of the [root] is itself.
		 */
		val tail: Id
			get() =
				if (parts.isEmpty()) this
				else Id(parts.subList(1, parts.size))

		override fun toString() = parts.joinToString(separator = ":")

		companion object {
			val root = Id(emptyList())

			/**
			 * Parses [id] into a value of the [Id] class.
			 *
			 * This function accepts the same format as [Id.toString] generates.
			 */
			fun fromString(id: String) =
				if (id.isEmpty()) root
				else Id(id.split(':').map { it.toInt() })
		}
	}

	//endregion

	companion object {
		//region Builders

		fun label(
			label: String,
		) = Label(
			label,
			importedFrom = null,
		)

		fun input(
			label: String,
			input: opensavvy.formulaide.core.Input,
		) = Input(
			label,
			input,
			importedFrom = null,
		)

		fun choice(
			label: String,
			vararg fields: Pair<Int, Field>,
		) = Choice(
			label,
			mapOf(*fields),
			importedFrom = null,
		)

		fun group(
			label: String,
			vararg fields: Pair<Int, Field>,
		) = Group(
			label,
			mapOf(*fields),
			importedFrom = null,
		)

		fun arity(
			label: String,
			range: UIntRange,
			field: Field,
		) = Arity(
			label,
			field,
			range,
			importedFrom = null,
		)

		//endregion
	}

}
