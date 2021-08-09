package formulaide.api.fields

import formulaide.api.types.Arity
import formulaide.api.types.Ref
import formulaide.api.types.Ref.Companion.createRef
import formulaide.api.types.Ref.Companion.ids
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import formulaide.api.data.Composite as CompositeData

/**
 * A field in a [form][FormRoot], that matches a field in a [composite data structure][CompositeData].
 *
 * The [id] and [name] must match with the corresponding field, however the [arity] is allowed to be different (but shouldn't conflict).
 */
@Serializable
sealed class DeepFormField : FormField, Field.Reference<DataField> {

	/**
	 * The [DataField] that models the data of this [DeepFormField].
	 * @see dataField
	 * @see DeepFormField
	 */
	abstract override val ref: Ref<DataField>

	/**
	 * The [DataField] that models the data of this [DeepFormField].
	 *
	 * @throws Ref.UnloadedReferenceException if [ref] hasn't been loaded (see [loadRef])
	 */
	// Abstract so the different implementations can override the type safely
	abstract val dataField: DataField

	/**
	 * The identifier of the referenced [DataField].
	 * @see ref
	 */
	override val id get() = ref.id

	/**
	 * The name of the referenced [DataField].
	 * @see ref
	 */
	override val name get() = dataField.name // will throw if the reference is not loaded

	/**
	 * The order of the referenced [DataField].
	 *
	 * @see ref
	 */
	override val order get() = dataField.order // will throw if the reference is not loaded

	/**
	 * Loads the [ref] to the [CompositeData] that this field mirrors.
	 */
	fun loadRef(parentFields: List<DataField>, allowNotFound: Boolean, lazy: Boolean) {
		ref.loadFrom(parentFields, allowNotFound, lazy)
	}

	override fun load(composites: List<CompositeData>, allowNotFound: Boolean, lazy: Boolean) {}
	override fun validate() {
		require(ref.loaded) { "Ce champ de formulaire aurait déjà du être chargé (cf. loadRef)" }

		require(arity.min >= dataField.arity.min) { "Un champ d'un formulaire ne peut pas accepter moins de réponses que le champ de la donnée correspondante ; la donnée (${ref.obj.id}, '${ref.obj.name}') accepte a une arité de ${ref.obj.arity} et le champ accepte une arité de $arity" }
		require(arity.max <= dataField.arity.max) { "Un champ d'un formulaire ne peut pas accepter plus de réponses que le champ de la donnée correspondante ; la donnée (${ref.obj.id}, '${ref.obj.name}') accepte a une arité de ${ref.obj.arity} et le champ accepte une arité de $arity" }
	}

	/**
	 * A field that represents a single data entry, matching a [DataField].
	 *
	 * For more information, see [DeepFormField] and [Field.Simple].
	 */
	@Serializable
	@SerialName("FORM_SIMPLE_DEEP")
	data class Simple(
		override val ref: Ref<DataField>,
		override val simple: SimpleField,
	) : DeepFormField(), FormField.Simple {

		constructor(ref: DataField.Simple, simple: SimpleField) : this(ref.createRef(),
		                                                               simple)

		override val dataField: DataField.Simple
			get() {
				return ref.obj as? DataField.Simple
					?: error("Ce champ est de type SIMPLE, mais il référence un champ de type ${ref.obj::class}")
			}

		override fun validate() {
			super.validate()

			dataField.simple.validateCompatibility(simple)
		}

		override fun toString() = "Deep.Simple($ref, $simple)" + super.toString()
		override fun requestCopy(name: String?, arity: Arity?) =
			copy(simple = simple.requestCopy(arity))
	}

	/**
	 * A field that allows the user to choose between multiple [options].
	 *
	 * For more information, see [DeepFormField] and [Field.Union].
	 */
	@Serializable
	@SerialName("FORM_UNION_DEEP")
	data class Union(
		override val ref: Ref<DataField>,
		override val arity: Arity,
		override val options: List<DeepFormField>,
	) : DeepFormField(), FormField.Union<DeepFormField> {

		constructor(
			ref: DataField.Union,
			arity: Arity,
			options: List<DeepFormField>,
		) : this(ref.createRef(), arity, options)

		override val dataField
			get() = ref.obj as? DataField.Union
				?: error("Ce champ est de type UNION, mais il référence un champ de type ${ref.obj::class}")

		override fun load(
			composites: List<CompositeData>,
			allowNotFound: Boolean,
			lazy: Boolean,
		) {
			super.load(composites, allowNotFound, lazy)

			// Load the corresponding DataField.Union, just in case
			dataField.load(composites, allowNotFound, lazy = true)

			options.forEach { it.loadRef(dataField.options, allowNotFound, lazy = true) }
		}

		override fun validate() {
			super.validate()

			require(dataField.options.ids() == options.ids()) { "Ce champ est de type UNION, et référence un champ ayant comme options ${dataField.options} mais n'autorise que les options $options" }
		}

		override fun toString() = "Deep.Union($ref, $arity, $options)" + super.toString()
		override fun requestCopy(name: String?, arity: Arity?) = copy(arity = arity ?: this.arity)
	}

	/**
	 * A field that corresponds to a [composite data structure][DataField.Composite].
	 *
	 * For more information, see [DeepFormField], [ShallowFormField.Composite] and [DataField.Composite].
	 */
	@Serializable
	@SerialName("FORM_COMPOSITE_DEEP")
	data class Composite(
		override val ref: Ref<DataField>,
		override val arity: Arity,
		override val fields: List<DeepFormField>,
	) : DeepFormField(), FormField.Composite {

		constructor(
			ref: DataField.Composite,
			arity: Arity,
			fields: List<DeepFormField>,
		) : this(ref.createRef(), arity, fields)

		override val dataField
			get() = ref.obj as? DataField.Composite
				?: error("Ce champ est de type COMPOSITE, mais il référence un champ de type ${ref.obj::class}")

		override fun load(
			composites: List<CompositeData>,
			allowNotFound: Boolean,
			lazy: Boolean,
		) {
			super.load(composites, allowNotFound, lazy)

			// Load the corresponding DataField.Composite, just in case
			dataField.load(composites, allowNotFound, lazy = true)

			fields.forEach { it.loadRef(dataField.ref.obj.fields, allowNotFound, lazy = true) }
		}

		override fun validate() {
			super.validate()

			val dataField = ref.obj
			require(dataField is DataField.Composite) { "Ce champ est de type COMPOSITE, mais il référence un champ de type ${dataField::class}" }
		}

		override fun toString() = "Deep.Composite($ref, $arity, $fields)" + super.toString()
		override fun requestCopy(name: String?, arity: Arity?) = copy(arity = arity ?: this.arity)
	}

	override fun toString(): String = if (ref.loaded) " -> $dataField" else ""

	companion object {

		fun DataField.createMatchingFormField(composites: List<CompositeData>): DeepFormField =
			when (this) {
				is DataField.Simple -> Simple(this.createRef(), simple)
				is DataField.Union -> Union(
					this,
					arity,
					options.map { it.createMatchingFormField(composites) }
				)
				is DataField.Composite -> Composite(this, Arity.forbidden(), emptyList())
			}

	}
}
