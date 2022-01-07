package formulaide.ui.components.inputs

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

			+props.text
		}

		props.children()
	}
}
