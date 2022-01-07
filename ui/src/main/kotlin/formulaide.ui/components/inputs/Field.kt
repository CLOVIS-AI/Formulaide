package formulaide.ui.components.inputs

import formulaide.ui.components.text.Text
import react.FC
import react.PropsWithChildren
import react.dom.html.ReactHTML.label

external interface FieldProps : PropsWithChildren {
	var id: String
	var text: String
}

val Field = FC<FieldProps>("Field") { props ->
	FormField {
		label {
			className = "block"
			htmlFor = props.id

			Text { text = props.text }
		}

		props.children()
	}
}
