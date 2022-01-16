package formulaide.ui.fields.renderers

import formulaide.api.fields.FormField
import formulaide.ui.components.inputs.Nesting
import react.FC

/**
 * Renders a [FormField.Composite], so the user can fill it in.
 */
val CompositeFormFieldRenderer = FC<FieldProps>("CompositeFormFieldRenderer") { props ->
	val field = props.field
	require(field is FormField.Composite) { "CompositeFormFieldRenderer: expected a FormField.Composite, but found a ${field::class}: $field" }

	val subFields = field.fields

	Nesting {
		for (subField in subFields) {
			Field {
				+props

				this.field = subField
				this.id = "${props.idOrDefault}:${subField.id}"
			}
		}
	}
}
