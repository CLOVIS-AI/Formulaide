package formulaide.api.fields

import formulaide.api.data.Form
import formulaide.api.fields.FormField.Shallow
import formulaide.api.types.Arity
import formulaide.api.types.Ref
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import formulaide.api.fields.FormField.Composite as FormComposite
import formulaide.api.fields.FormField.Shallow.Composite as ShallowComposite

/**
 * The root of a form field tree, which represents the [fields][FormField] of a form.
 *
 * In a form, [FormField.Deep.Composite] references a [formulaide.api.data.Composite] and is able to override some of its properties.
 * All of its children must then recursively reference a matching [DataField] (see [FormField.Deep]).
 */
@Serializable
data class FormRoot(
	val fields: List<Shallow>,
)

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
	sealed interface Union<F : FormField> : FormField, Field.Union<F>

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
		 * A field that represents a single data entry.
		 *
		 * For more information, see [Field.Simple].
		 */
		@SerialName("FORM_SIMPLE_SHALLOW")
		data class Simple(
			override val id: String,
			override val order: Int,
			override val name: String,
			override val simple: SimpleField,
		) : Shallow(), FormField.Simple

		/**
		 * A field that allows the user to choose between multiple [options].
		 *
		 * For more information, see [Field.Union].
		 */
		@SerialName("FORM_UNION_SHALLOW")
		data class Union(
			override val id: String,
			override val order: Int,
			override val name: String,
			override val arity: Arity,
			override val options: List<Shallow>,
		) : Shallow(), FormField.Union<Shallow>

		/**
		 * A field that corresponds to a [composite data structure][formulaide.api.data.Composite].
		 *
		 * All of its children must reference the corresponding data structure as well: see [Deep].
		 */
		@SerialName("FORM_COMPOSITE_SHALLOW")
		data class Composite(
			override val id: String,
			override val order: Int,
			override val name: String,
			override val arity: Arity,
			override val ref: Ref<ShallowComposite>,
			override val fields: List<Deep>,
		) : Shallow(), Field.Reference<ShallowComposite>,
		    FormComposite
	}

	/**
	 * A field in a [form][FormRoot], that matches a field in a [composite data structure][formulaide.api.data.Composite].
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

		/**
		 * A field that represents a single data entry, matching a [DataField].
		 *
		 * For more information, see [Deep] and [Field.Simple].
		 */
		@SerialName("FORM_SIMPLE_DEEP")
		data class Simple(
			override val ref: Ref<DataField>,
			override val simple: SimpleField,
		) : Deep(), FormField.Simple

		/**
		 * A field that allows the user to choose between multiple [options].
		 *
		 * For more information, see [Deep] and [Field.Union].
		 */
		@SerialName("FORM_UNION_DEEP")
		data class Union(
			override val ref: Ref<DataField>,
			override val arity: Arity,
			override val options: List<Deep>,
		) : Deep(), FormField.Union<Deep>

		/**
		 * A field that corresponds to a [composite data structure][DataField.Composite].
		 *
		 * For more information, see [Deep], [ShallowComposite] and [DataField.Composite].
		 */
		@SerialName("FORM_COMPOSITE_DEEP")
		data class Composite(
			override val ref: Ref<DataField>,
			override val arity: Arity,
			override val fields: List<Deep>,
		) : Deep(), FormComposite
	}

}
