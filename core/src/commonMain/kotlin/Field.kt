package opensavvy.formulaide.core

import opensavvy.backbone.Ref.Companion.now
import opensavvy.formulaide.core.Field.*
import opensavvy.formulaide.core.Field.Input
import opensavvy.state.outcome.Outcome
import opensavvy.state.outcome.ensureValid
import opensavvy.state.outcome.out

private const val INDENT = "    "

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
	 * The entire field tree, including this field itself and all its children, recursively.
	 */
	val tree: Sequence<Pair<Id, Field>>
		get() = sequence {
			yield(Id.root to this@Field)

			for ((id, child) in indexedFields) {
				child.tree
					.map { (subId, subField) -> (Id(id) + subId) to subField }
					.also { yieldAll(it) }
			}
		}

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

	protected fun validateSync() {
		require(label.isNotBlank()) { "Le libellé d'un champ ne peut pas être vide : '$label'" }
	}

	protected abstract suspend fun validateCompatibleWith(source: Field): Outcome<Unit>

	/**
	 * Checks the validity of this field.
	 */
	suspend fun validate(): Outcome<Unit> = out {
		validateSync()

		importedFrom?.let { templateVersionRef ->
			val version = templateVersionRef.now().bind()
			val source = version.field

			validateCompatibleWith(source).bind()
		}

		fields.forEach { it.validate().bind() }
	}

	protected abstract fun StringBuilder.toString(indent: String)

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
			validateSync()
		}

		override suspend fun validateCompatibleWith(source: Field): Outcome<Unit> = out {
			ensureValid(source is Label) { "Impossible d'importer un label à partir d'un ${source::class} (${this@Label} -> $source)" }
		}

		override fun StringBuilder.toString(indent: String) {
			append(label)
		}

		override fun toString() = buildString { toString("") }
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
			validateSync()
		}

		override suspend fun validateCompatibleWith(source: Field): Outcome<Unit> = out {
			ensureValid(source is Input) { "Impossible d'importer une saisie à partir d'un ${source::class} (${this@Input} -> $source)" }

			input.validateCompatibleWith(source.input).bind()
		}

		override fun StringBuilder.toString(indent: String) {
			append(label)
			append(", $input")
		}

		override fun toString() = buildString { toString("") }
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
			validateSync()
		}

		override suspend fun validateCompatibleWith(source: Field): Outcome<Unit> = out {
			ensureValid(source is Choice) { "Impossible d'importer un choix à partir d'un ${source::class} (${this@Choice} -> $source)" }

			// Each option of the imported field MUST exist in the source field
			// However, the imported field is allowed to remove options from the source
			for ((id, child) in indexedFields) {
				val sourceChild = source.indexedFields[id]
				ensureValid(sourceChild != null) { "Le champ importé ${this@Choice} ne peut pas ajouter une option à sa source, il n'est donc pas possible d'ajouter ${child.label} ($id)" }

				child.validateCompatibleWith(sourceChild).bind()
			}
		}

		override fun StringBuilder.toString(indent: String) {
			append(label)
			append(" (choix)")

			val subIndent = indent + INDENT

			for ((id, option) in indexedFields) {
				appendLine()
				append(indent)
				append("$id.".padEnd(INDENT.length))
				with(option) { toString(subIndent) }
			}
		}

		override fun toString() = buildString { toString("") }
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
			validateSync()
		}

		override suspend fun validateCompatibleWith(source: Field): Outcome<Unit> = out {
			ensureValid(source is Group) { "Impossible d'importer un groupe à partir d'un ${source::class} (${this@Group} -> $source)" }

			// The subfields should exactly match with the source
			for ((id, child) in indexedFields) {
				val sourceChild = source.indexedFields[id]
				ensureValid(sourceChild != null) { "Le champ importé ${this@Group} ne peut pas ajouter une réponse à sa source, il n'est donc pas possible d'ajouter ${child.label} ($id)" }

				child.validateCompatibleWith(sourceChild).bind()
			}

			for ((id, sourceChild) in source.indexedFields) {
				val child = indexedFields[id]
				ensureValid(child != null) { "Le champ importé ${this@Group} ne peut pas supprimer une réponse de sa source, la réponse ${sourceChild.label} ($id) est manquante" }
			}
		}

		override fun StringBuilder.toString(indent: String) {
			append(label)
			append(" (groupe)")

			val subIndent = indent + INDENT

			for ((id, option) in indexedFields) {
				appendLine()
				append(indent)
				append("$id.".padEnd(INDENT.length))
				with(option) { toString(subIndent) }
			}
		}

		override fun toString() = buildString { toString("") }
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
			validateSync()

			// Answering the same field more than 100 times is probably a mistake
			require(allowed.last < 100u) { "il n'est pas possible de répondre à un même champ plus de 100 fois, vous avez demandé ${allowed.last}" }
		}

		override val indexedFields: Map<Int, Field>
			get() = List(allowed.last.toInt()) { it to child }.toMap()

		override suspend fun validateCompatibleWith(source: Field): Outcome<Unit> = out {
			ensureValid(source is Arity) { "Impossible d'importer un label à partir d'un ${source::class} (${this@Arity} -> $source)" }

			ensureValid(allowed.first >= source.allowed.first) { "Le champ importé ${this@Arity} ne peut pas autoriser moins de réponses (minimum ${allowed.first}) que sa source (minimum ${source.allowed.first}), champ ${source.label}" }
			ensureValid(allowed.last <= source.allowed.last) { "Le champ importé ${this@Arity} ne peut pas autoriser plus de réponses (maximum ${allowed.last}) que sa source (maximum ${source.allowed.last}), champ ${source.label}" }

			child.validateCompatibleWith(source.child).bind()
		}

		override fun StringBuilder.toString(indent: String) {
			append(label)
			appendLine(" (de ${allowed.first} à ${allowed.last})")

			append(indent)
			append(INDENT)
			with(child) { toString(indent + INDENT) }
		}

		override fun toString() = buildString { toString("") }
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

		fun labelFrom(
			template: Template.Version.Ref,
			label: String,
		) = Label(
			label,
			importedFrom = template,
		)

		fun input(
			label: String,
			input: opensavvy.formulaide.core.Input,
		) = Input(
			label,
			input,
			importedFrom = null,
		)

		fun inputFrom(
			template: Template.Version.Ref,
			label: String,
			input: opensavvy.formulaide.core.Input,
		) = Input(
			label,
			input,
			template,
		)

		fun choice(
			label: String,
			vararg fields: Pair<Int, Field>,
		) = Choice(
			label,
			mapOf(*fields),
			importedFrom = null,
		)

		fun choiceFrom(
			template: Template.Version.Ref,
			label: String,
			vararg fields: Pair<Int, Field>,
		) = Choice(
			label,
			mapOf(*fields),
			template,
		)

		fun group(
			label: String,
			vararg fields: Pair<Int, Field>,
		) = Group(
			label,
			mapOf(*fields),
			importedFrom = null,
		)

		fun groupFrom(
			template: Template.Version.Ref,
			label: String,
			vararg fields: Pair<Int, Field>,
		) = Group(
			label,
			mapOf(*fields),
			template,
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

		fun arityFrom(
			template: Template.Version.Ref,
			label: String,
			range: UIntRange,
			field: Field,
		) = Arity(
			label,
			field,
			range,
			template,
		)

		//endregion
	}

}
