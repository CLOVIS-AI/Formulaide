package formulaide.core.field

import formulaide.core.field.Field.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.collections.List as KotlinList

/**
 * Local identifier for a field.
 *
 * There are multiple ways to uniquely identify a field:
 * - Globally, with a pair of [Field.Container] and [Field.Id],
 * - Within a given container, with a [Field.Id],
 * - Among the direct children of a specific field, with [LocalFieldId].
 *
 * No two children of the same field can share the same [LocalFieldId].
 * Other than that, the value doesn't have any meaning.
 */
typealias LocalFieldId = Int

/**
 * A field in a form or template.
 *
 * Fields are recursive data structures (see [indexedFields]).
 * Different types of fields exist:
 * - [labels][Label] display text to a user,
 * - [inputs][Input] ask the user to provide information as a simple non-recursive data type,
 * - [choices][Choice] let the user select one option among multiple,
 * - [groups][Group] bundle related fields into a single compound field,
 * - [lists][List] manage fields that can be filled in multiple times, as well as optional fields.
 *
 * Fields are stored as part of the [Container] class.
 *
 * It is possible to reuse fields from a template in other templates or in forms (see [sourceId]).
 */
sealed class Field {

	/**
	 * The label of this field.
	 *
	 * The label is displayed to the user to explain to them what they should fill in.
	 * It should not be blank.
	 */
	abstract val label: String

	/**
	 * Children fields of this field.
	 *
	 * Subfields are indexed by the [LocalFieldId].
	 * Fields should be displayed in the same order as they appear in this dictionary.
	 *
	 * @see fields
	 */
	abstract val indexedFields: Map<LocalFieldId, Field>

	/**
	 * Children fields of this field.
	 *
	 * @see indexedFields
	 */
	val fields: Sequence<Field>
		get() = indexedFields
			.values
			.asSequence()

	/**
	 * The source of this field.
	 *
	 * When [sourceId] is `null`, this field is a regular field.
	 * When [sourceId] is not `null`, this field is an imported fields.
	 *
	 * Imported fields allow to reuse fields from a template in another template or in a form.
	 * Imported fields have the same attributes as regular fields, however the constraints added to their values must be stricter than their source.
	 * Mathematically speaking, the set of values accepted by a field must be a subset of the set of values accepted by their source.
	 *
	 * This attribute stores a pair of the [Container] from which the other field comes from, as well as the [Id] of the source.
	 */
	abstract val sourceId: Pair<Container, Id>?

	val source by lazy {
		sourceId?.let { (container, id) ->
			container[id] ?: error("L'identifiant de la source du champ '$label' n'existe pas dans le conteneur source")
		}
	}

	// Internal function to check that all values are correct
	protected fun verify() {
		require(label.isNotBlank()) { "Le label d'un champ ne peut pas être vide : '$label'" }

		// If there is a source, check that our subfields match the source's subfields
		source?.let { source ->
			// Check that they have the same IDs
			// -> adding a new field to an imported field is forbidden
			// -> removing a field when importing makes the imported field incompatible with its source
			require(indexedFields.keys == source.indexedFields.keys) { "Le champ importé '$label' devrait posséder les mêmes sous-champs que sa source ; la source possède les sous-champs d'identifiants ${source.indexedFields.keys} mais ce champ possède les sous-champs d'identifiants ${indexedFields.keys}" }

			for ((id, field) in indexedFields) {
				val fieldSource = source.indexedFields[id]

				require(field.source == fieldSource) { "Le sous-champ '$field' ($id) du champ '$label' a comme source ${field.source}, qui n'est pas le sous-champ du même identifiant apparaissant dans sa source ($fieldSource)" }
			}
		}
	}

	fun child(id: LocalFieldId): Field? = indexedFields[id]

	fun child(id: KotlinList<LocalFieldId>): Field? = when {
		id.isEmpty() -> this
		else -> child(id[0])?.child(id.subList(1, id.size))
	}

	//region Types

	/**
	 * The unit field.
	 *
	 * Visually, on the [label] is displayed.
	 * No input is requested of the user.
	 *
	 * This type is used to embed non-interactive information into a form (legal text, ...).
	 *
	 * Labels never have children.
	 */
	data class Label(
		override val label: String,
		override val sourceId: Pair<Container, Id>? = null,
	) : Field() {
		override val indexedFields: Map<LocalFieldId, Field>
			get() = emptyMap()

		init {
			verify()

			if (sourceId != null)
				require(source is Label) { "Un label ne peut pas avoir comme source un champ de type différent : $source" }
		}

		override fun toString() = "Label($label${sourceId.toShortString()})"
	}

	/**
	 * A leaf field.
	 *
	 * Inputs never have children.
	 */
	data class Input(
		override val label: String,
		val input: InputConstraints,
		override val sourceId: Pair<Container, Id>? = null,
	) : Field() {
		override val indexedFields: Map<LocalFieldId, Field>
			get() = emptyMap()

		init {
			verify()

			if (sourceId != null) {
				val source = source
				require(source is Input) { "Une entrée utilisateur ne peut pas avoir comme source un champ de type différent : $source" }
				input.requireCompatibleWith(source.input)
			}
		}

		override fun toString() = "Input($label, $input${sourceId.toShortString()})"
	}

	/**
	 * Multiple choices among which the user must select one.
	 */
	data class Choice(
		override val label: String,
		override val indexedFields: Map<LocalFieldId, Field>,
		override val sourceId: Pair<Container, Id>? = null,
	) : Field() {

		init {
			verify()

			if (sourceId != null)
				require(source is Choice) { "Un choix ne peut pas avoir comme source un champ de type différent : $source" }
		}

		override fun toString() = "Choice($label, $indexedFields${sourceId.toShortString()})"
	}

	/**
	 * A grouping of multiple related fields that the user must all fill in.
	 */
	data class Group(
		override val label: String,
		override val indexedFields: Map<LocalFieldId, Field>,
		override val sourceId: Pair<Container, Id>? = null,
	) : Field() {

		init {
			verify()

			if (sourceId != null)
				require(source is Group) { "Un groupe ne peut pas avoir comme source un champ de type différent : $source" }
		}

		override fun toString() = "Group($label, $indexedFields${sourceId.toShortString()})"
	}

	/**
	 * Controls whether fields are mandatory or optional.
	 *
	 * By default, all fields are mandatory.
	 * To make a field optional, wrap it in a [List] with [allowed] to `0..1` (zero or one response are accepted.)
	 *
	 * Other kinds of ranges can be used to create fields that can be filled in multiple times.
	 * For example, `3..5` would mean a field that must be filled at least 3 times, but at most 5 times.
	 *
	 * This can be useful to avoid repetition.
	 */
	data class List(
		override val label: String,
		val field: Field,
		val allowed: UIntRange,
		override val sourceId: Pair<Container, Id>? = null,
	) : Field() {
		init {
			verify()

			// lists with more than 100 elements are forbidden because they are likely a conversion mistake from Int to UInt
			// why would anyone ask the user to fill in the same field 100+ times?
			require(allowed.last < 100u) { "Il n'est pas possible de répondre à un même champ plus de 100 fois, vous avez demandé ${allowed.last} fois" }

			if (sourceId != null)
				require(source is List) { "Une liste ne peut pas avoir comme source un champ de type différent : $source" }
		}

		override val indexedFields: Map<LocalFieldId, Field> = KotlinList(allowed.last.toInt()) { it to field }.toMap()

		override fun toString() = "List($label, $allowed $field${sourceId.toShortString()})"
	}

	//endregion
	//region Global identification

	/**
	 * The object that stores a field tree.
	 *
	 * This object is generally found as part of a form or a template.
	 */
	data class Container(
		val id: String,
		val name: String,
		val root: Field,
	) {
		operator fun get(id: Id): Field? = root.child(id.parts)
	}

	/**
	 * Identifier for a field inside a [Container].
	 *
	 * @see LocalFieldId
	 * @see Container
	 */
	@Serializable(with = Id.Serializer::class)
	data class Id(val parts: KotlinList<LocalFieldId>) {
		override fun toString() = parts.joinToString(":")

		operator fun plus(id: LocalFieldId) = Id(parts + id)

		companion object {
			fun idOf(vararg part: LocalFieldId) = Id(parts = part.toList())
		}

		internal object Serializer : KSerializer<Id> {
			override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Field.Id", PrimitiveKind.STRING)

			override fun deserialize(decoder: Decoder): Id {
				val parts = decoder.decodeString().split(":")
					.map { it.toInt() }
				return Id(parts)
			}

			override fun serialize(encoder: Encoder, value: Id) {
				encoder.encodeString(value.parts.joinToString(separator = ":"))
			}
		}
	}

	//endregion
	//region Utilities

	protected fun Pair<Container, Id>?.toShortString() = when (this) {
		null -> ""
		else -> {
			val (container, id) = this
			", sourceContainer=${container.id} '${container.name}', sourceField=$id"
		}
	}

	//endregion
}
