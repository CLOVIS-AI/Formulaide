@file:Suppress("DuplicatedCode")

package formulaide.ui.fields.editors

import formulaide.api.fields.Field
import formulaide.api.fields.SimpleField
import formulaide.ui.components.inputs.Field
import react.FC
import react.dom.html.InputType

val IntegerMetadataEditor = FC<EditableFieldProps>("IntegerMetadataEditor") { props ->
	val field = props.field as Field.Simple
	val simple = field.simple as SimpleField.Integer

	val minId = metadataEditorId(props.uniqueId, "min")
	val maxId = metadataEditorId(props.uniqueId, "max")

	Field {
		id = minId
		text = "Valeur minimale"

		MetadataEditorInput {
			+props

			type = InputType.number
			id = minId

			current = simple.min
			onUpdate = { input -> input.value.toLongOrNull()?.let { simple.copy(min = it) } }
		}

		MetadataEditorResetButton {
			+props

			onReset = { simple.copy(min = null) }
		}
	}

	Field {
		id = maxId
		text = "Valeur maximale"

		MetadataEditorInput {
			+props

			type = InputType.number
			id = maxId

			current = simple.max
			onUpdate = { input -> input.value.toLongOrNull()?.let { simple.copy(max = it) } }
		}

		MetadataEditorResetButton {
			+props

			onReset = { simple.copy(max = null) }
		}
	}
}
