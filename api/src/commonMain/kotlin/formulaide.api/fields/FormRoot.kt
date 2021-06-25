package formulaide.api.fields

import formulaide.api.types.Arity
import formulaide.api.types.Ref
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The root of a form field tree, which represents the [fields][FormField] of a form.
 *
 * In a form, [FormField.Composite] references a [formulaide.api.data.Composite] and is able to override some of its properties.
 * All of its children must then recursively reference a matching [DataField] (see [DeepFormField]).
 */
@Serializable
data class FormRoot(
	val fields: List<FormField>,
) {

	/**
	 * Marker interface for simple fields that appear in forms.
	 */
	sealed interface SimpleFormField : Field.Simple

	/**
	 * Marker interface for union fields that appear in forms.
	 */
	sealed interface UnionFormField<F : Field> : Field.Union<F>

	/**
	 * Marker interface to composite fields that appear in forms.
	 */
	sealed interface CompositeFormField : Field.Container<DeepFormField>
}

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
		override val order: Int,
		override val name: String,
		override val simple: SimpleField,
	) : FormField(), FormRoot.SimpleFormField

	/**
	 * A field that allows the user to choose between multiple [options].
	 *
	 * For more information, see [Field.Union].
	 */
	@SerialName("FORM_UNION")
	data class Union(
		override val id: String,
		override val order: Int,
		override val name: String,
		override val arity: Arity,
		override val options: List<FormField>,
	) : FormField(), FormRoot.UnionFormField<FormField>

	/**
	 * A field that corresponds to a [composite data structure][formulaide.api.data.Composite].
	 *
	 * All of its children must reference the corresponding data structure as well: see [DeepFormField].
	 */
	@SerialName("FORM_COMPOSITE")
	data class Composite(
		override val id: String,
		override val order: Int,
		override val name: String,
		override val arity: Arity,
		override val ref: Ref<Composite>,
		override val fields: List<DeepFormField>,
	) : FormField(), Field.Reference<Composite>, FormRoot.CompositeFormField
}

/**
 * A field in a [form][FormRoot], that matches a field in a [composite data structure][formulaide.api.data.Composite].
 *
 * The [id] and [name] must match with the corresponding field, however the [arity] is allowed to be different (but shouldn't conflict).
 */
@Serializable
sealed class DeepFormField : Field, Field.Reference<DataField> {

	/**
	 * The [DataField] that models the data of this [DeepFormField].
	 * @see DeepFormField
	 */
	abstract override val ref: Ref<DataField>

	/**
	 * The identifier of the referenced [DataField].
	 * @see ref
	 */
	override val id get() = ref.id

	/**
	 * The name of the referenced [DataField].
	 * @see ref
	 */
	override val name get() = ref.obj.name // will throw if the reference is not loaded

	/**
	 * The order of the referenced [DataField].
	 *
	 * @see ref
	 */
	override val order get() = ref.obj.order // will throw if the reference is not loaded

	/**
	 * A field that represents a single data entry, matching a [DataField].
	 *
	 * For more information, see [DeepFormField] and [Field.Simple].
	 */
	@SerialName("FORM_SIMPLE_DEEP")
	data class Simple(
		override val ref: Ref<DataField>,
		override val simple: SimpleField,
	) : DeepFormField(), FormRoot.SimpleFormField

	/**
	 * A field that allows the user to choose between multiple [options].
	 *
	 * For more information, see [DeepFormField] and [Field.Union].
	 */
	@SerialName("FORM_UNION_DEEP")
	data class Union(
		override val ref: Ref<DataField>,
		override val arity: Arity,
		override val options: List<DeepFormField>,
	) : DeepFormField(), FormRoot.UnionFormField<DeepFormField>

	/**
	 * A field that corresponds to a [composite data structure][DataField.Composite].
	 *
	 * For more information, see [DeepFormField], [FormField.Composite] and [DataField.Composite].
	 */
	@SerialName("FORM_COMPOSITE_DEEP")
	data class Composite(
		override val ref: Ref<DataField>,
		override val arity: Arity,
		override val fields: List<DeepFormField>,
	) : DeepFormField(), FormRoot.CompositeFormField
}
