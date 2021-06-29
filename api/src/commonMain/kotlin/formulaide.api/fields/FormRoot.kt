package formulaide.api.fields

import formulaide.api.data.Form
import formulaide.api.fields.FormField.Shallow
import formulaide.api.types.Arity
import formulaide.api.types.Ref
import formulaide.api.types.Ref.Companion.createRef
import formulaide.api.types.Ref.Companion.ids
import formulaide.api.types.Ref.Companion.loadIfNecessary
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import formulaide.api.data.Composite as CompositeData
import formulaide.api.fields.FormField.Composite as FormComposite
import formulaide.api.fields.FormField.Shallow.Composite as ShallowComposite

/**
 * The root of a form field tree, which represents the [fields][FormField] of a form.
 *
 * In a form, [FormField.Deep.Composite] references a [CompositeData] and is able to override some of its properties.
 * All of its children must then recursively reference a matching [DataField] (see [FormField.Deep]).
 *
 * @see formulaide.api.dsl.formRoot
 */
@Serializable
data class FormRoot(
	val fields: List<Shallow>,
) {

	/**
	 * Validates and loads references in this [FormRoot].
	 */
	fun validate(composites: List<CompositeData>) {
		fields.forEach { it.validate(composites) }
	}
}

/**
 * Marker interface for fields that appear in [forms][Form].
 *
 * Fields from the [FormRoot] to the first [FormComposite] are modeled
 */
sealed interface FormField : Field {

	/**
	 * Marker interface for simple fields that appear in forms.
	 */
	sealed interface Simple : FormField, Field.Simple

	/**
	 * Marker interface for union fields that appear in forms.
	 */
	sealed interface Union<out F : FormField> : FormField, Field.Union<F>

	/**
	 * Marker interface to composite fields that appear in forms.
	 */
	sealed interface Composite : FormField, Field.Container<Deep>

	/**
	 * Fields that do not have a transitive [ShallowComposite] parent.
	 *
	 * @see Deep
	 */
	@Serializable
	sealed class Shallow : FormField {

		/**
		 * Loads [references][Ref] contained in this [FormField] (recursively).
		 */
		abstract fun validate(composites: List<CompositeData>)

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
		) : Shallow(), FormField.Simple {

			override fun validate(composites: List<CompositeData>) =
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
			override val options: List<Shallow>,
		) : Shallow(), FormField.Union<Shallow> {

			override fun validate(composites: List<CompositeData>) =
				options.forEach { it.validate(composites) }
		}

		/**
		 * A field that corresponds to a [composite data structure][CompositeData].
		 *
		 * All of its children must reference the corresponding data structure as well: see [Deep].
		 */
		@Serializable
		@SerialName("FORM_COMPOSITE_SHALLOW")
		data class Composite(
			override val id: String,
			override val order: Int,
			override val name: String,
			override val arity: Arity,
			override val ref: Ref<CompositeData>,
			override val fields: List<Deep>,
		) : Shallow(), Field.Reference<CompositeData>, FormComposite {

			override fun validate(composites: List<CompositeData>) {
				ref.loadIfNecessary(composites)
				fields.forEach { it.validate(ref.obj.fields, composites) }
			}
		}

		fun copyToSimple(simple: SimpleField) = Simple(id, order, name, simple)
		fun copyToUnion(options: List<Shallow>) = Union(id, order, name, arity, options)
		fun copyToComposite(composite: Ref<CompositeData>, fields: List<Deep>) = Composite(id, order, name, arity, composite, fields)
	}

	/**
	 * A field in a [form][FormRoot], that matches a field in a [composite data structure][CompositeData].
	 *
	 * The [id] and [name] must match with the corresponding field, however the [arity] is allowed to be different (but shouldn't conflict).
	 */
	@Serializable
	sealed class Deep : FormField, Field.Reference<DataField> {

		/**
		 * The [DataField] that models the data of this [Deep].
		 * @see Deep
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

		open fun validate(fields: List<DataField>, composites: List<CompositeData>) {
			ref.loadIfNecessary(fields)

			require(arity.min >= ref.obj.arity.min) { "Un champ d'un formulaire ne peut pas accepter moins de réponses que le champ de la donnée correspondante ; la donnée (${ref.obj.id}, '${ref.obj.name}') accepte a une arité de ${ref.obj.arity} et le champ accepte une arité de $arity" }
			require(arity.max <= ref.obj.arity.max) { "Un champ d'un formulaire ne peut pas accepter plus de réponses que le champ de la donnée correspondante ; la donnée (${ref.obj.id}, '${ref.obj.name}') accepte a une arité de ${ref.obj.arity} et le champ accepte une arité de $arity" }
		}

		/**
		 * A field that represents a single data entry, matching a [DataField].
		 *
		 * For more information, see [Deep] and [Field.Simple].
		 */
		@Serializable
		@SerialName("FORM_SIMPLE_DEEP")
		data class Simple(
			override val ref: Ref<DataField>,
			override val simple: SimpleField,
		) : Deep(), FormField.Simple {

			constructor(ref: DataField.Simple, simple: SimpleField) : this(ref.createRef(), simple)

			override fun validate(
				fields: List<DataField>,
				composites: List<CompositeData>
			) {
				super.validate(fields, composites)

				val obj = ref.obj
				require(obj is DataField.Simple) { "Ce champ est de type SIMPLE, mais il référence un champ de type ${obj::class}" }
				obj.simple.validateCompatibility(simple)
			}
		}

		/**
		 * A field that allows the user to choose between multiple [options].
		 *
		 * For more information, see [Deep] and [Field.Union].
		 */
		@Serializable
		@SerialName("FORM_UNION_DEEP")
		data class Union(
			override val ref: Ref<DataField>,
			override val arity: Arity,
			override val options: List<Deep>,
		) : Deep(), FormField.Union<Deep> {

			constructor(ref: DataField.Union, arity: Arity, options: List<Deep>) : this(ref.createRef(), arity, options)

			override fun validate(
				fields: List<DataField>,
				composites: List<CompositeData>
			) {
				super.validate(fields, composites)

				val dataField = ref.obj
				require(dataField is DataField.Union) { "Ce champ est de type UNION, mais il référence un champ de type ${dataField::class}" }

				require(dataField.options.ids() == options.ids()) { "Ce champ est de type UNION, et référence un champ ayant comme options ${dataField.options} mais n'autorise que les options $options" }

				options.forEach { it.validate(dataField.options, composites) }
			}
		}

		/**
		 * A field that corresponds to a [composite data structure][DataField.Composite].
		 *
		 * For more information, see [Deep], [ShallowComposite] and [DataField.Composite].
		 */
		@Serializable
		@SerialName("FORM_COMPOSITE_DEEP")
		data class Composite(
			override val ref: Ref<DataField>,
			override val arity: Arity,
			override val fields: List<Deep>,
		) : Deep(), FormComposite {

			constructor(ref: DataField.Composite, arity: Arity, fields: List<Deep>) : this(ref.createRef(), arity, fields)

			override fun validate(
				fields: List<DataField>,
				composites: List<CompositeData>
			) {
				super.validate(fields, composites)

				val dataField = ref.obj
				require(dataField is DataField.Composite) { "Ce champ est de type COMPOSITE, mais il référence un champ de type ${dataField::class}" }

				// Load the corresponding DataField.Composite
				val referencedComposite = dataField.ref
				referencedComposite.loadIfNecessary(composites)

				this.fields.forEach { it.validate(referencedComposite.obj.fields, composites) }
			}
		}
	}

}
