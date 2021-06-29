package formulaide.api.fields

import formulaide.api.fields.DataField.Composite
import formulaide.api.types.Arity
import formulaide.api.types.Ref
import formulaide.api.types.Ref.Companion.loadIfNecessary
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import formulaide.api.data.Composite as CompositeData

/**
 * A field in a [composite data structure][CompositeData].
 *
 * Data fields do not recurse on [Composite], instead they simply refer to another composite data structure (implicit recursion).
 */
@Serializable
sealed class DataField : Field {

	/**
	 * Checks that all constraints of this [DataField] are respected, and loads all references.
	 */
	abstract fun validate(composites: List<CompositeData>)

	/**
	 * A field that represents a single data entry.
	 *
	 * For more information, see [Field.Simple].
	 */
	@Serializable
	@SerialName("DATA_SIMPLE")
	data class Simple(
		override val id: String,
		override val order: Int,
		override val name: String,
		override val simple: SimpleField,
	) : DataField(), Field.Simple {

		override fun validate(composites: List<CompositeData>) {} // Nothing to do
	}

	/**
	 * A field that allows the user to choose between multiple [options].
	 *
	 * For more information, see [Field.Union].
	 */
	@Serializable
	@SerialName("DATA_UNION")
	data class Union(
		override val id: String,
		override val order: Int,
		override val name: String,
		override val arity: Arity,
		override val options: List<DataField>,
	) : DataField(), Field.Union<DataField> {

		override fun validate(composites: List<CompositeData>) {
			options.forEach { it.validate(composites) }
		}
	}

	/**
	 * A field that represents another composite data structure.
	 *
	 * Composite data structures only [reference][ref] other composite data structures, and cannot
	 * override their settings (unlike [forms][FormField.Shallow.Composite]).
	 *
	 * Because there is no recursion here, this class doesn't implement [Field.Container].
	 *
	 * @property ref The [Composite][CompositeData] this object represents.
	 */
	@Serializable
	@SerialName("DATA_REFERENCE")
	data class Composite(
		override val id: String,
		override val order: Int,
		override val name: String,
		override val arity: Arity,
		override val ref: Ref<CompositeData>,
	) : DataField(), Field.Reference<CompositeData> {

		override fun validate(composites: List<CompositeData>) {
			ref.loadIfNecessary(composites)
		}
	}

	fun copyToSimple(simple: SimpleField) = Simple(id, order, name, simple)
	fun copyToUnion(options: List<DataField>) = Union(id, order, name, arity, options)
	fun copyToComposite(composite: Ref<CompositeData>) = Composite(id, order, name,  arity, composite)
}
