package formulaide.ui.components.inputs

import formulaide.ui.components.text.Text
import react.FC
import react.dom.html.InputType
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.label

external interface CheckboxProps : InputProps {
	var text: String
}

val Checkbox = FC<CheckboxProps>("Checkbox") { props ->
	input {
		type = InputType.hidden
		name = props.id
		value = "false"
	}

	SmallInput {
		+props
		type = InputType.checkbox
		value = "true"
	}

	label {
		Text { text = props.text }
		htmlFor = props.id
	}
}
