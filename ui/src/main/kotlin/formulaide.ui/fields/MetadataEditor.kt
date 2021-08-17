package formulaide.ui.fields

import formulaide.api.fields.Field
import formulaide.api.fields.SimpleField
import formulaide.api.fields.SimpleField.Upload.Format
import formulaide.api.fields.SimpleField.Upload.Format.*
import formulaide.ui.components.styledButton
import formulaide.ui.components.styledCheckbox
import formulaide.ui.components.styledField
import formulaide.ui.components.styledInput
import kotlinx.html.INPUT
import kotlinx.html.InputType
import kotlinx.html.js.onChangeFunction
import org.w3c.dom.HTMLInputElement
import react.RBuilder
import react.dom.div
import react.fc

val MetadataEditor = fc<EditableFieldProps> { props ->
	val field = props.field

	if (field is Field.Simple) {
		when (val simple = field.simple) {
			is SimpleField.Text -> {
				val id = idOf(field, "max-length")
				styledField(id, "Longueur maximale") {
					styledInput(InputType.number, id) {
						setHandler(simple.maxLength,
						           props,
						           field,
						           update = {
							           it.value.toIntOrNull()?.let { simple.copy(maxLength = it) }
						           })
						min = "0"
					}
					cancelButton(simple.maxLength,
					             props,
					             field,
					             update = { simple.copy(maxLength = null) })
				}
			}
			is SimpleField.Integer -> {
				val id = idOf(field, "min")
				styledField(id, "Valeur minimale") {
					styledInput(InputType.number, id) {
						setHandler(simple.min,
						           props,
						           field,
						           update = {
							           it.value.toLongOrNull()?.let { simple.copy(min = it) }
						           })
					}
					cancelButton(simple.min, props, field, update = { simple.copy(min = null) })
				}

				val id2 = idOf(field, "max")
				styledField(id2, "Valeur maximale") {
					styledInput(InputType.number, id2) {
						setHandler(simple.max,
						           props,
						           field,
						           update = {
							           it.value.toLongOrNull()?.let { simple.copy(max = it) }
						           })
					}
					cancelButton(simple.max, props, field, update = { simple.copy(max = null) })
				}
			}
			is SimpleField.Upload -> {
				val id = idOf(field, "files")
				styledField(id, "Formats autorisés") {
					for (format in Format.values()) {
						val name = format.displayName()
						val idFormat = idOf(field, name)
						div {
							styledCheckbox(
								idFormat,
								"$name (${format.extensions.joinToString(separator = ", ")})",
							) {
								checked = simple.allowedFormats.any { it == format }

								setHandler(
									simple.allowedFormats.find { it == format },
									props,
									field,
									update = {
										if (it.checked)
											simple.copy(allowedFormats = simple.allowedFormats + format)
										else
											simple.copy(allowedFormats = simple.allowedFormats - format)
									}
								)
							}
						}
					}
				}

				val idSize = idOf(field, "size")
				styledField(idSize, "Taille maximale (Mo)") {
					styledInput(InputType.number, idSize) {
						min = "1"
						max = "10"
						setHandler(simple.maxSizeMB,
						           props,
						           field,
						           update = {
							           it.value.toIntOrNull()?.let { simple.copy(maxSizeMB = it) }
						           })
					}
					cancelButton(simple.maxSizeMB,
					             props,
					             field,
					             update = { simple.copy(maxSizeMB = null) })
				}

				val idExpiration = idOf(field, "expiration")
				styledField(idExpiration, "Durée de vie avant suppression (jours)") {
					styledInput(InputType.number, idExpiration) {
						min = "1"
						max = "5000"
						setHandler(simple.expiresAfterDays,
						           props,
						           field,
						           update = {
							           it.value.toIntOrNull()
								           ?.let { simple.copy(expiresAfterDays = it) }
						           })
					}
					cancelButton(simple.expiresAfterDays,
					             props,
					             field,
					             update = { simple.copy(expiresAfterDays = null) })
				}
			}
		}
	}
}

private fun idOf(field: Field, attributeName: String) =
	"field-editor-metadata-${field.id}-$attributeName"

private fun RBuilder.cancelButton(
	value: Any?,
	props: EditableFieldProps,
	field: Field.Simple,
	update: () -> SimpleField,
) {
	if (value != null)
		styledButton("×", action = { props.replace(field.requestCopy(update())) })
}

private fun INPUT.setHandler(
	value: Any?,
	props: EditableFieldProps,
	field: Field.Simple,
	update: (HTMLInputElement) -> SimpleField?,
) {
	this.value = value?.toString() ?: ""
	this.onChangeFunction = {
		val target = it.target as HTMLInputElement
		val updated = update(target)
		if (updated != null)
			props.replace(field.requestCopy(updated))
	}
}

private fun Format.displayName() = when (this) {
	IMAGE -> "Images"
	DOCUMENT -> "Documents"
	ARCHIVE -> "Archives"
	AUDIO -> "Audio"
	VIDEO -> "Vidéo"
	TABULAR -> "Tableurs"
	EVENT -> "Événements"
}
