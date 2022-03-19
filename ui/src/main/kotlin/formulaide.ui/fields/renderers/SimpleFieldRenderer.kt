package formulaide.ui.fields.renderers

import formulaide.api.fields.Field
import formulaide.api.fields.SimpleField
import formulaide.api.types.Arity
import formulaide.ui.components.inputs.Checkbox
import formulaide.ui.components.inputs.Input
import formulaide.ui.components.inputs.InputProps
import formulaide.ui.components.text.ErrorText
import kotlinx.js.jso
import react.FC
import react.dom.html.InputType
import react.useState

/**
 * Renders a [Field.Simple], so the user can fill it in.
 */
val SimpleFieldRenderer = FC<FieldProps>("SimpleFieldRenderer") { props ->
	val field = props.field
	require(field is Field.Simple) { "SimpleFieldRenderer: expected a Field.Simple, but found a ${field::class}: $field" }
	val simple = field.simple

	var simpleInputValue by useState<String>()

	val defaultInputProps = jso<InputProps> {
		id = props.idOrDefault
		name = props.idOrDefault
		required = field.arity == Arity.mandatory()
		onChange = {
			val newValue = it.target.value
			simpleInputValue = newValue
			props.onInput?.invoke(props.fieldKeyOrDefault, newValue)
		}
		if (simple.defaultValue != null)
			placeholder = simple.defaultValue.toString()
	}

	when (simple) {
		is SimpleField.Text -> Input { +defaultInputProps; type = InputType.text }
		is SimpleField.Integer -> Input {
			+defaultInputProps
			type = InputType.number

			min = simple.min?.toDouble()
			max = simple.max?.toDouble()
		}
		is SimpleField.Decimal -> Input { +defaultInputProps; type = InputType.number; step = 0.01 }
		is SimpleField.Boolean -> Checkbox {
			name = props.idOrDefault
			id = props.idOrDefault
			text = ""
			required = false
		}
		is SimpleField.Message -> Unit // The message only has a label, not a body
		is SimpleField.Email -> Input { +defaultInputProps; type = InputType.email }
		is SimpleField.Phone -> Input { +defaultInputProps; type = InputType.tel }
		is SimpleField.Date -> Input { +defaultInputProps; type = InputType.date }
		is SimpleField.Time -> Input { +defaultInputProps; type = InputType.time }
		is SimpleField.Upload -> UploadFieldRenderer { +props; value = simpleInputValue }
	}

	// If the given value is NOT empty, check that the value is legal
	// We don't check for empty values, otherwise the form would originally be entirely empty
	if (!simpleInputValue.isNullOrBlank())
		try {
			simple.parse(simpleInputValue)
		} catch (e: Exception) {
			ErrorText { text = " ${e.message}" }
		}
}
