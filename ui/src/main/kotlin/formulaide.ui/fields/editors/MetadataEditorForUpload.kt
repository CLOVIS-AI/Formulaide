package formulaide.ui.fields.editors

import formulaide.api.fields.Field
import formulaide.api.fields.SimpleField
import formulaide.ui.components.inputs.Checkbox
import formulaide.ui.components.inputs.Field
import react.FC
import react.dom.html.InputType
import react.dom.html.ReactHTML.div

val UploadMetadataEditor = FC<EditableFieldProps>("UploadMetadataEditor") { props ->
	val field = props.field as Field.Simple
	val simple = field.simple as SimpleField.Upload

	val filesId = metadataEditorId(props.uniqueId, "files")
	val sizeId = metadataEditorId(props.uniqueId, "size")
	val expirationId = metadataEditorId(props.uniqueId, "expiration")

	Field {
		id = filesId
		text = "Formats autorisés"

		for (format in SimpleField.Upload.Format.values()) {
			val name = format.displayName()
			val formatId = metadataEditorId(props.uniqueId, "format-$format")

			div {
				Checkbox {
					id = formatId
					text = "$name (${format.extensions.joinToString(", ")})"

					checked = simple.allowedFormats.any { it == format }
					value = simple.allowedFormats.find { it == format }?.toString() ?: ""

					onChange = {
						val target = it.target
						val updated =
							if (target.checked) simple.copy(allowedFormats = simple.allowedFormats + format)
							else simple.copy(allowedFormats = simple.allowedFormats - format)
						props.replace(field.requestCopy(updated))
					}
				}
			}
		}
	}

	Field {
		id = sizeId
		text = "Taille maximale (Mo)"

		MetadataEditorInput {
			+props

			type = InputType.number
			id = sizeId
			min = 1.0
			max = 10.0

			current = simple.maxSizeMB
			default = simple.effectiveMaxSizeMB

			onUpdate = { html -> html.value.toIntOrNull()?.let { simple.copy(maxSizeMB = it) } }
		}

		MetadataEditorResetButton {
			+props

			onReset = { simple.copy(maxSizeMB = null) }
		}
	}

	Field {
		id = expirationId
		text = "Durée de vie avant suppression (jours)"

		MetadataEditorInput {
			+props

			type = InputType.number
			id = expirationId
			min = 1.0
			max = 5000.0

			current = simple.expiresAfterDays
			default = simple.effectiveExpiresAfterDays

			onUpdate = { html -> html.value.toIntOrNull()?.let { simple.copy(expiresAfterDays = it) } }
		}

		MetadataEditorResetButton {
			+props

			onReset = { simple.copy(expiresAfterDays = null) }
		}
	}
}

private fun SimpleField.Upload.Format.displayName() = when (this) {
	SimpleField.Upload.Format.IMAGE -> "Images"
	SimpleField.Upload.Format.DOCUMENT -> "Documents"
	SimpleField.Upload.Format.ARCHIVE -> "Archives"
	SimpleField.Upload.Format.AUDIO -> "Audio"
	SimpleField.Upload.Format.VIDEO -> "Vidéo"
	SimpleField.Upload.Format.TABULAR -> "Tableurs"
	SimpleField.Upload.Format.EVENT -> "Événements"
}
