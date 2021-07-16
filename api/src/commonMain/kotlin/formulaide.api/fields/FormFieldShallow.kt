package formulaide.api.fields

import formulaide.api.fields.ShallowFormField.Composite
import formulaide.api.types.Arity
import formulaide.api.types.Ref
import formulaide.api.types.Ref.Companion.loadIfNecessary
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Fields that do not have a transitive [Composite] parent.
 *
 * @see DeepFormField
 */
@Serializable
sealed class ShallowFormField : FormField {

	/**
	 * Loads [references][Ref] contained in this [FormField] (recursively).
	 */
	abstract fun validate(composites: List<formulaide.api.data.Composite>)

	/**
	 * A field that represents a single data entry.
	 *
	 * For more information, see [Field.Simple].
	 */
	@Serializable
	@SerialName("FORM_SIMPLE_SHALLOW")
	data class Simple(
		override val id: String,
		override val order: Int,
		override val name: String,
		override val simple: SimpleField,
	) : ShallowFormField(), FormField.Simple {

		override fun validate(composites: List<formulaide.api.data.Composite>) =
			Unit // Nothing to do
	}

	/**
	 * A field that allows the user to choose between multiple [options].
	 *
	 * For more information, see [Field.Union].
	 */
	@Serializable
	@SerialName("FORM_UNION_SHALLOW")
	data class Union(
		override val id: String,
		override val order: Int,
		override val name: String,
		override val arity: Arity,
		override val options: List<ShallowFormField>,
	) : ShallowFormField(), FormField.Union<ShallowFormField> {

		override fun validate(composites: List<formulaide.api.data.Composite>) =
			options.forEach { it.validate(composites) }
	}

	/**
	 * A field that corresponds to a [composite data structure][formulaide.api.data.Composite].
	 *
	 * All of its children must reference the corresponding data structure as well: see [DeepFormField].
	 */
	@Serializable
	@SerialName("FORM_COMPOSITE_SHALLOW")
	data class Composite(
		override val id: String,
		override val order: Int,
		override val name: String,
		override val arity: Arity,
		override val ref: Ref<formulaide.api.data.Composite>,
		override val fields: List<DeepFormField>,
	) : ShallowFormField(), Field.Reference<formulaide.api.data.Composite>, FormField.Composite {

		override fun validate(composites: List<formulaide.api.data.Composite>) {
			ref.loadIfNecessary(composites)
			fields.forEach { it.validate(ref.obj.fields, composites) }
		}
	}

	fun copyToSimple(simple: SimpleField) = Simple(id, order, name, simple)
	fun copyToUnion(options: List<ShallowFormField>) = Union(id, order, name, arity, options)
	fun copyToComposite(
		composite: Ref<formulaide.api.data.Composite>,
		fields: List<DeepFormField>,
	) = Composite(id, order, name, arity, composite, fields)
}
