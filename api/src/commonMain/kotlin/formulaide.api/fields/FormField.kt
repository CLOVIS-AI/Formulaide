package formulaide.api.fields

import formulaide.api.data.Form
import formulaide.api.types.Ref
import kotlinx.serialization.Serializable
import formulaide.api.data.Composite as CompositeData
import formulaide.api.fields.FormField.Composite as FormComposite

/**
 * The root of a form field tree, which represents the [fields][FormField] of a form.
 *
 * In a form, [DeepFormField.Composite] references a [CompositeData] and is able to override some of its properties.
 * All of its children must then recursively reference a matching [DataField] (see [DeepFormField]).
 *
 * @see formulaide.api.dsl.formRoot
 */
@Serializable
data class FormRoot(
	val fields: List<ShallowFormField>,
) {

	/**
	 * Loads the different fields in this root (recursively).
	 *
	 * Parameters [allowNotFound] and [lazy] are passed to [Ref.loadFrom].
	 */
	fun load(
		composites: List<CompositeData>,
		allowNotFound: Boolean = false,
		lazy: Boolean = true,
	) {
		fieldMonad().forEach { it.load(composites, allowNotFound, lazy) }
	}

	/**
	 * Validates this [FormRoot].
	 */
	fun validate() {
		fieldMonad().forEach { it.validate() }
	}
}

/**
 * Marker interface for fields that appear in [forms][Form].
 *
 * Fields from the [FormRoot] to the first [FormComposite] are modeled
 */
sealed interface FormField : Field {

	/**
	 * Loads the references contained by this [FormField].
	 *
	 * This function is not recursive; see [fieldMonad].
	 */
	fun load(composites: List<CompositeData>, allowNotFound: Boolean = false, lazy: Boolean = true)

	/**
	 * Checks that this field validates all of its constraints.
	 *
	 * The field should be [loaded][load] before this function is called.
	 * This function is not recursive; see [fieldMonad].
	 */
	fun validate()

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
	sealed interface Composite : FormField, Field.Container<DeepFormField>

}
