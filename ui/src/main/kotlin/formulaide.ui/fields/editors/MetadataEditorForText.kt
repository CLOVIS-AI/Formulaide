package formulaide.ui.fields.editors

import formulaide.api.fields.Field
import formulaide.api.fields.SimpleField
import formulaide.ui.components.inputs.Field
import react.FC
import react.dom.html.InputType

val TextMetadataEditor = FC<EditableFieldProps>("TextMetadataEditor") { props ->
	val id = metadataEditorId(props.uniqueId, "max-length")

	val field = props.field as Field.Simple
	val simple = field.simple as SimpleField.Text

	Field {
		this.id = id
		text = "Longueur maximale"

		MetadataEditorInput {
			+props

			type = InputType.number
			this.id = id
			min = 0.0

			current = simple.maxLength
			onUpdate = { input -> input.value.toIntOrNull()?.let { simple.copy(maxLength = it) } }
		}

		MetadataEditorResetButton {
			+props
			onReset = { simple.copy(maxLength = null) }
		}
	}
}
