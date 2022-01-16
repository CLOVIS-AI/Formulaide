package formulaide.ui.fields.renderers

import formulaide.api.fields.Field
import formulaide.api.fields.SimpleField
import formulaide.ui.components.inputs.FormField
import formulaide.ui.components.inputs.Nesting
import formulaide.ui.components.inputs.RadioButton
import react.FC
import react.useMemo
import react.useState

/**
 * Renders a [Field.Union], so the user can fill it in.
 */
val UnionFieldRenderer = FC<FieldProps>("UnionFieldRenderer") { props ->
	val field = props.field
	require(field is Field.Union<*>) { "UnionFieldRenderer: expected a Field.Union, but found a ${field::class}: $field" }

	val subFields = useMemo(field.options) { field.options.sortedBy { it.order } }
	val (selected, setSelected) = useState(subFields.first())

	Nesting {
		FormField {
			for (subField in subFields) {
				RadioButton {
					radioId = props.idOrDefault
					buttonId = "${props.fieldKey}-${subField.id}"
					value = subField.id
					text = subField.name
					checked = subField === selected
					onClick = { setSelected(subField) }
				}
			}
		}

		if (selected !is Field.Simple || selected.simple != SimpleField.Message) {
			Field {
				+props

				this.field = selected
				this.id = "${props.idOrDefault}:${selected.id}"
			}
		}
	}
}
