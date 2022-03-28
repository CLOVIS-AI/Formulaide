package formulaide.ui.components.inputs

import formulaide.ui.utils.classes
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
			classes = "block"
			htmlFor = props.id

			+props.text
		}

		+props.children
	}
}
