package formulaide.core.field

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import opensavvy.backbone.Backbone
import opensavvy.backbone.Ref.Companion.requestValue

/**
 * Flat representation of the [Field] hierarchy used in the API.
 *
 * This representation mostly impacts the source (see [Field.sourceId]): instead of being a Kotlin reference to another
 * object, this class only stores the identifier of the other field.
 *
 * Conversion to a [Field] instance is done via [resolve].
 * Conversion from a [Field] instance is done via [flatten].
 */
@Serializable
sealed class FlatField {

	/**
	 * The label's field.
	 *
	 * @see Field.label
	 */
	abstract val label: String

	/**
	 * The source container of this field.
	 *
	 * @see Field.source
	 */
	abstract val sourceContainer: Container.Ref?

	/**
	 * The source field of this field.
	 *
	 * @see Field.source
	 */
	abstract val sourceField: Field.Id?

	@Serializable
	class Label(
		override val label: String,
		override val sourceContainer: @Contextual Container.Ref? = null,
		override val sourceField: Field.Id? = null,
	) : FlatField()

	@Serializable
	class Input(
		override val label: String,
		val constraints: InputConstraints,
		override val sourceContainer: @Contextual Container.Ref? = null,
		override val sourceField: Field.Id? = null,
	) : FlatField()

	@Serializable
	class Choice(
		override val label: String,
		val options: Map<LocalFieldId, FlatField>,
		override val sourceContainer: @Contextual Container.Ref? = null,
		override val sourceField: Field.Id? = null,
	) : FlatField()

	@Serializable
	class Group(
		override val label: String,
		val fields: Map<LocalFieldId, FlatField>,
		override val sourceContainer: @Contextual Container.Ref? = null,
		override val sourceField: Field.Id? = null,
	) : FlatField()

	@Serializable
	class List(
		override val label: String,
		val field: FlatField,
		/**
		 * Minimum allowed number of responses.
		 */
		val min: UInt,
		/**
		 * Maximum allowed number of responses (inclusive).
		 */
		val max: UInt,
		override val sourceContainer: @Contextual Container.Ref? = null,
		override val sourceField: Field.Id? = null,
	) : FlatField()

	@Serializable
	class Container(
		@SerialName("_id") val id: String,
		val root: FlatField,
	) {

		data class Ref(val id: String, override val backbone: FieldBackbone) : opensavvy.backbone.Ref<Container> {
			override fun toString() = "Fields $id"
		}
	}
}

interface FieldBackbone : Backbone<FlatField.Container> {

	/**
	 * Creates a new field container.
	 *
	 * Only administrators can create new fields.
	 */
	suspend fun create(name: String, root: Field): FlatField.Container.Ref
}

//region Conversion FlatField -> Field

private suspend fun resolveSource(
	container: FlatField.Container.Ref?,
	field: Field.Id?,
): Pair<Field.Container, Field.Id>? =
	if (container == null) {
		require(field == null) { "Le conteneur est 'null' mais le champ ne l'est pas : $field" }
		null
	} else {
		requireNotNull(field) { "Le conteneur est non-nul ($container), mais le champ l'est : $field" }
		val source = container.requestValue().resolve()
		source to field
	}

suspend fun FlatField.resolve(): Field = when (this) {
	is FlatField.Choice -> Field.Choice(
		label,
		options.mapValues { (_, v) -> v.resolve() },
		resolveSource(sourceContainer, sourceField)
	)

	is FlatField.Group -> Field.Group(
		label,
		fields.mapValues { (_, v) -> v.resolve() },
		resolveSource(sourceContainer, sourceField)
	)

	is FlatField.Input -> Field.Input(label, constraints, resolveSource(sourceContainer, sourceField))
	is FlatField.Label -> Field.Label(label, resolveSource(sourceContainer, sourceField))
	is FlatField.List -> Field.List(label, field.resolve(), min..max, resolveSource(sourceContainer, sourceField))
}

suspend fun FlatField.Container.resolve() = Field.Container(
	id,
	root.resolve()
)

//endregion
//region Conversion Field -> FlatField

private fun Pair<Field.Container, Field.Id>?.flatten(bone: FieldBackbone): Pair<FlatField.Container.Ref?, Field.Id?> =
	if (this == null)
		null to null
	else {
		val (container, field) = this
		FlatField.Container.Ref(container.id, bone) to field
	}

fun Field.flatten(bone: FieldBackbone): FlatField {
	val (container, field) = sourceId.flatten(bone)

	return when (this) {
		is Field.Choice -> FlatField.Choice(
			label,
			indexedFields.mapValues { (_, v) -> v.flatten(bone) },
			container,
			field
		)

		is Field.Group -> FlatField.Group(
			label,
			indexedFields.mapValues { (_, v) -> v.flatten(bone) },
			container,
			field
		)

		is Field.Input -> FlatField.Input(label, input, container, field)
		is Field.Label -> FlatField.Label(label, container, field)
		is Field.List -> FlatField.List(label, this.field.flatten(bone), allowed.first, allowed.last, container, field)
	}
}

fun Field.Container.flatten(bone: FieldBackbone) = FlatField.Container(
	id,
	root.flatten(bone),
)

//endregion
