package formulaide.ui.components.inputs

import react.FC
import react.PropsWithChildren

external interface FieldEditorShellProps : PropsWithChildren {
	var id: String
	var text: String
}

val FieldEditorShell = FC<FieldEditorShellProps>("FieldEditorShell") { props ->
	Field {
		this.id = props.id
		this.text = props.text

		Nesting {
			props.children()
		}
	}
}
