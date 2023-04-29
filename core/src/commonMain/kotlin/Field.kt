package opensavvy.formulaide.core

import arrow.core.EitherNel
import arrow.core.flatten
import arrow.core.mapOrAccumulate
import arrow.core.nonEmptyListOf
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import opensavvy.backbone.now
import opensavvy.formulaide.core.Field.*
import opensavvy.formulaide.core.Field.Failures.Compatibility.IncompatibleField.Companion.invalidMaxArity
import opensavvy.formulaide.core.Field.Failures.Compatibility.IncompatibleField.Companion.invalidMinArity
import opensavvy.formulaide.core.Field.Failures.Compatibility.IncompatibleField.Companion.invalidType
import opensavvy.formulaide.core.Field.Failures.Compatibility.IncompatibleField.Companion.tooFewFields
import opensavvy.formulaide.core.Field.Failures.Compatibility.IncompatibleField.Companion.tooManyFields
import opensavvy.formulaide.core.Field.Input
import opensavvy.state.arrow.toEither
import opensavvy.state.failure.CustomFailure
import opensavvy.state.failure.Failure
import opensavvy.state.failure.NotFound
import kotlin.reflect.KClass

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

	protected abstract suspend fun validateCompatibleWith(
		source: Field,
		id: Id,
	): EitherNel<Failures.Compatibility, Unit>

	/**
	 * Checks the validity of this field.
	 */
	suspend fun validate() = validate(Id.root)

	private suspend fun validate(id: Id): EitherNel<Failures.Compatibility, Unit> = either {
		validateSync()

		importedFrom?.let { templateVersionRef ->
			val version = templateVersionRef.now()
				.toEither()
				.mapLeft { nonEmptyListOf(Failures.Compatibility.TemplateNotFound(id, templateVersionRef)) }
				.bind()
			val source = version.field

			validateCompatibleWith(source, id).bind()
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

		override suspend fun validateCompatibleWith(source: Field, id: Id) = either {
			ensure(source is Label) { invalidType(id, this@Label::class, source::class) }
		}.mapLeft { nonEmptyListOf(it) }

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

		override suspend fun validateCompatibleWith(source: Field, id: Id) = either {
			ensure(source is Input) { invalidType(id, this@Input::class, source::class) }

			input.validateCompatibleWith(source.input)
				.toEither()
				.mapLeft { Failures.Compatibility.IncompatibleInput(id, it) }
				.bind()
		}.mapLeft { nonEmptyListOf(it) }

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

		override suspend fun validateCompatibleWith(source: Field, id: Id): EitherNel<Failures.Compatibility, Unit> = either {
			ensure(source is Choice) { nonEmptyListOf(invalidType(id, this@Choice::class, source::class)) }

			// Each option of the imported field MUST exist in the source field
			// However, the imported field is allowed to remove options from the source
			indexedFields.mapOrAccumulate { (childId, child) ->
				val sourceChild = source.indexedFields[childId]
				ensureNotNull(sourceChild) { nonEmptyListOf(tooManyFields(id, child, id + childId)) }

				child.validateCompatibleWith(sourceChild, id + childId).bind()
			}.mapLeft { it.flatten() }
				.bind()
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

		override suspend fun validateCompatibleWith(source: Field, id: Id): EitherNel<Failures.Compatibility, Unit> = either {
			ensure(source is Group) { nonEmptyListOf(invalidType(id, this@Group::class, source::class)) }

			// The subfields should exactly match with the source
			indexedFields.mapOrAccumulate { (childId, child) ->
				val sourceChild = source.indexedFields[childId]
				ensureNotNull(sourceChild) { nonEmptyListOf(tooManyFields(id, child, id + childId)) }

				child.validateCompatibleWith(sourceChild, id + childId).bind()
			}.mapLeft { it.flatten() }
				.bind()

			// Checking that all children of the imported field are still present
			source.indexedFields.mapOrAccumulate { (childId, sourceChild) ->
				val child = indexedFields[childId]
				ensureNotNull(child) { nonEmptyListOf(tooFewFields(id, sourceChild, id + childId)) }
			}.mapLeft { it.flatten() }
				.bind()
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

		override suspend fun validateCompatibleWith(source: Field, id: Id): EitherNel<Failures.Compatibility, Unit> = either {
			ensure(source is Arity) { nonEmptyListOf(invalidType(id, this@Arity::class, source::class)) }

			ensure(allowed.first >= source.allowed.first) { nonEmptyListOf(invalidMinArity(id, allowed.first, source)) }
			ensure(allowed.last <= source.allowed.last) { nonEmptyListOf(invalidMaxArity(id, allowed.last, source)) }

			child.validateCompatibleWith(source.child, id + 0).bind()
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

	// endregion
	// region Failures

	sealed interface Failures {
		sealed interface FieldFailure : Failures {
			val field: Id
		}

		sealed interface Compatibility : FieldFailure {
			class IncompatibleField(
				override val field: Id,
				message: String,
			) : CustomFailure(Companion, "Champ $field : $message"),
				Compatibility,
				FieldFailure {

				companion object : Failure.Key {
					internal fun invalidType(
						field: Id,
						target: KClass<out Field>,
						from: KClass<out Field>,
					) = IncompatibleField(field, "impossible d'importer $target à partir de $from")

					internal fun tooManyFields(
						field: Id,
						additionalField: Field,
						additionalFieldId: Id,
					) = IncompatibleField(field, "il est interdit d'ajouter une option à sa source, il n'est donc pas possible d'ajouter “${additionalField.label}” ($additionalFieldId)")

					internal fun tooFewFields(
						field: Id,
						missingField: Field,
						missingFieldId: Id,
					) = IncompatibleField(field, "il est interdit d'enlever un champ présent dans la source, il n'est donc pas possible d'omettre “${missingField.label}” ($missingFieldId)")

					internal fun invalidMinArity(
						field: Id,
						minimum: UInt,
						sourceField: Arity,
					) = IncompatibleField(field, "il est interdit d'autoriser moins de réponses (minimum $minimum) que sa source (minimum ${sourceField.allowed.first}), champ “${sourceField.label}”")

					internal fun invalidMaxArity(
						field: Id,
						maximum: UInt,
						sourceField: Arity,
					) = IncompatibleField(field, "il est interdit d'autoriser plus de réponses (maximum ${maximum}) que sa source (maximum ${sourceField.allowed.last}), champ “${sourceField.allowed.last}”")
				}
			}

			data class IncompatibleInput(
				override val field: Id,
				val failure: opensavvy.formulaide.core.Input.Failures.Compatibility,
			) : Compatibility

			class TemplateNotFound(
				override val field: Id,
				template: Template.Version.Ref,
			) : CustomFailure(NotFound(template)),
				Compatibility
		}
	}

	// endregion

	companion object {
		// region Builders

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
