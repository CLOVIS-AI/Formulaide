package formulaide.api.fields

import formulaide.api.types.Arity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The root of a form field tree, which represents the [fields][FormField] of a form.
 *
 * In a form, [FormField.Composite] references a [DataRoot] and is able to override some of its properties.
 * All of its children must then recursively reference a matching [DataField] (see [DeepFormField]).
 */
@Serializable
data class FormRoot(
	val fields: List<FormField>,
)

/**
 * A field in a [form][FormRoot].
 */
@Serializable
sealed class FormField : Field {

	/**
	 * A field that represents a single data entry.
	 *
	 * For more information, see [Field.Simple].
	 */
	@SerialName("FORM_SIMPLE")
	data class Simple(
		override val id: String,
		override val name: String,
		override val simple: SimpleField,
	) : FormField(), Field.Simple

	/**
	 * A field that allows the user to choose between multiple [options].
	 *
	 * For more information, see [Field.Union].
	 */
	@SerialName("FORM_UNION")
	data class Union(
		override val id: String,
		override val name: String,
		override val arity: Arity,
		override val options: List<FormField>,
	) : FormField(), Field.Union<FormField>

	/**
	 * A field that corresponds to a [composite data structure][DataRoot].
	 *
	 * All of its children must reference the corresponding data structure as well: see [DeepFormField].
	 */
	@SerialName("FORM_COMPOSITE")
	data class Composite(
		override val id: String,
		override val name: String,
		override val arity: Arity,
		override val ref: DataRoot,
		override val fields: List<DeepFormField>,
	) : FormField(), Field.Reference<DataRoot>, Field.Container<DeepFormField>
}

/**
 * A field in a [form][FormRoot], that matches a field in a [composite data structure][DataRoot].
 *
 * The [id] and [name] must match with the corresponding field, however the [arity] is allowed to be different (but shouldn't conflict).
 */
@Serializable
sealed class DeepFormField : Field, Field.Reference<DataField> {

	/**
	 * The [DataField] that models the data of this [DeepFormField].
	 * @see DeepFormField
	 */
	abstract override val ref: DataField

	/**
	 * The identifier of the referenced [DataField].
	 * @see ref
	 */
	override val id get() = ref.id

	/**
	 * The name of the referenced [DataField].
	 * @see ref
	 */
	override val name get() = ref.name

	/**
	 * A field that represents a single data entry, matching a [DataField].
	 *
	 * For more information, see [DeepFormField] and [Field.Simple].
	 */
	@SerialName("FORM_SIMPLE_DEEP")
	data class Simple(
		override val ref: DataField.Simple,
		override val simple: SimpleField,
	) : DeepFormField(), Field.Simple

	/**
	 * A field that allows the user to choose between multiple [options].
	 *
	 * For more information, see [DeepFormField] and [Field.Union].
	 */
	@SerialName("FORM_UNION_DEEP")
	data class Union(
		override val ref: DataField.Union,
		override val arity: Arity,
		override val options: List<DeepFormField>,
	) : DeepFormField(), Field.Union<DeepFormField>

	/**
	 * A field that corresponds to a [composite data structure][DataField.Composite].
	 *
	 * For more information, see [DeepFormField], [FormField.Composite] and [DataField.Composite].
	 */
	@SerialName("FORM_COMPOSITE_DEEP")
	data class Composite(
		override val ref: DataField.Composite,
		override val arity: Arity,
		override val fields: List<DeepFormField>,
	) : DeepFormField(), Field.Container<DeepFormField>
}
