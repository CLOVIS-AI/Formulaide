package formulaide.ui.fields.editors

import formulaide.api.fields.Field
import formulaide.api.fields.SimpleField
import formulaide.ui.components.StyledButton
import formulaide.ui.components.inputs.Input
import formulaide.ui.components.inputs.InputProps
import org.w3c.dom.HTMLInputElement
import react.FC

external interface MetadataEditorResetButtonProps : EditableFieldProps {
	var value: Any?
	var onReset: () -> SimpleField
}

val MetadataEditorResetButton = FC<MetadataEditorResetButtonProps>("MetadataEditorResetButton") { props ->
	val field = props.field
	require(field is Field.Simple) { "MetadataEditorResetButton: expected Field.Simple, found ${field::class}: $field" }

	if (props.value != null) {
		StyledButton {
			text = "×"
			action = { props.replace(field.requestCopy(props.onReset())) }
		}
	}
}

external interface MetadataEditorInputProps : InputProps, EditableFieldProps {
	var current: Any?
	var default: Any?
	var onUpdate: (HTMLInputElement) -> SimpleField?
}

val MetadataEditorInput = FC<MetadataEditorInputProps>("MetadataEditorInput") { props ->
	val field = props.field as Field.Simple

	Input {
		+props

		value = props.current?.toString() ?: ""

		if (props.default != null)
			placeholder = props.default?.toString()

		onChange = {
			props.onUpdate(it.target)?.let { updated ->
				props.replace(field.requestCopy(updated))
			}
		}
	}
}
