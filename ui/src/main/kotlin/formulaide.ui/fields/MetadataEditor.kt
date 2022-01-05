package formulaide.ui.fields

import formulaide.api.fields.Field
import formulaide.api.fields.SimpleField
import formulaide.api.fields.SimpleField.Upload.Format
import formulaide.api.fields.SimpleField.Upload.Format.*
import formulaide.api.types.Arity
import formulaide.ui.components.styledButton
import formulaide.ui.components.styledCheckbox
import formulaide.ui.components.styledField
import formulaide.ui.components.styledInput
import org.w3c.dom.HTMLInputElement
import react.ChildrenBuilder
import react.FC
import react.dom.html.InputHTMLAttributes
import react.dom.html.InputType
import react.dom.html.ReactHTML.div
import react.useMemo

val MetadataEditor = FC<EditableFieldProps>("MetadataEditor") { props ->
	val field = props.field
	val defaultField =
		useMemo(field) { field.requestCopy(name = "Valeur par défaut", arity = Arity.optional()) }

	if (field is Field.Simple) {
		when (val simple = field.simple) {
			is SimpleField.Text -> textMetadata(field, simple, props)
			is SimpleField.Integer -> integerMetadata(field, simple, props)
			is SimpleField.Upload -> uploadMetadata(field, simple, props)

			// Fields that do not have metadata
			is SimpleField.Decimal, is SimpleField.Boolean, is SimpleField.Email, is SimpleField.Phone, is SimpleField.Date, is SimpleField.Time, is SimpleField.Message -> Unit
		}

		if (field.simple !is SimpleField.Upload && field.simple != SimpleField.Message) {
			field(
				form = null,
				root = null,
				defaultField,
				input = { _, value ->
					props.replace(
						field.requestCopy(
							field.simple.requestCopy(defaultValue = value)
						).requestCopy(name = "Valeur par défaut")
					)
				}
			)
		}
	}
}

private fun ChildrenBuilder.uploadMetadata(
	field: Field.Simple,
	simple: SimpleField.Upload,
	props: EditableFieldProps,
) {
	val id = idOf(props.uniqueId, "files")
	styledField(id, "Formats autorisés") {
		for (format in values()) {
			val name = format.displayName()
			val idFormat = idOf(props.uniqueId, name)
			div {
				styledCheckbox(
					idFormat,
					"$name (${format.extensions.joinToString(separator = ", ")})",
				) {
					checked = simple.allowedFormats.any { it == format }

					setHandler(
						simple.allowedFormats.find { it == format },
						null,
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

	val idSize = idOf(props.uniqueId, "size")
	styledField(idSize, "Taille maximale (Mo)") {
		styledInput(InputType.number, idSize) {
			min = 1.0
			max = 10.0
			setHandler(simple.maxSizeMB,
			           simple.effectiveMaxSizeMB,
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

	val idExpiration = idOf(props.uniqueId, "expiration")
	styledField(idExpiration, "Durée de vie avant suppression (jours)") {
		styledInput(InputType.number, idExpiration) {
			min = 1.0
			max = 5000.0
			setHandler(simple.expiresAfterDays,
			           simple.effectiveExpiresAfterDays,
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

private fun ChildrenBuilder.integerMetadata(
	field: Field.Simple,
	simple: SimpleField.Integer,
	props: EditableFieldProps,
) {
	val id = idOf(props.uniqueId, "min")
	styledField(id, "Valeur minimale") {
		styledInput(InputType.number, id) {
			setHandler(simple.min,
			           null,
			           props,
			           field,
			           update = {
				           it.value.toLongOrNull()?.let { simple.copy(min = it) }
			           })
		}
		cancelButton(simple.min, props, field, update = { simple.copy(min = null) })
	}

	val id2 = idOf(props.uniqueId, "max")
	styledField(id2, "Valeur maximale") {
		styledInput(InputType.number, id2) {
			setHandler(simple.max,
			           null,
			           props,
			           field,
			           update = {
				           it.value.toLongOrNull()?.let { simple.copy(max = it) }
			           })
		}
		cancelButton(simple.max, props, field, update = { simple.copy(max = null) })
	}
}

private fun ChildrenBuilder.textMetadata(
	field: Field.Simple,
	simple: SimpleField.Text,
	props: EditableFieldProps,
) {
	val id = idOf(props.uniqueId, "max-length")
	styledField(id, "Longueur maximale") {
		styledInput(InputType.number, id) {
			setHandler(simple.maxLength,
			           null,
			           props,
			           field,
			           update = {
				           it.value.toIntOrNull()?.let { simple.copy(maxLength = it) }
			           })
			min = 0.0
		}
		cancelButton(simple.maxLength,
		             props,
		             field,
		             update = { simple.copy(maxLength = null) })
	}
}

private fun idOf(uniqueId: String, attributeName: String) =
	"field-editor-metadata-${uniqueId}-$attributeName"

private fun ChildrenBuilder.cancelButton(
	value: Any?,
	props: EditableFieldProps,
	field: Field.Simple,
	update: () -> SimpleField,
) {
	if (value != null)
		styledButton("×", action = { props.replace(field.requestCopy(update())) })
}

private fun InputHTMLAttributes<HTMLInputElement>.setHandler(
	value: Any?,
	defaultValue: Any?,
	props: EditableFieldProps,
	field: Field.Simple,
	update: (HTMLInputElement) -> SimpleField?,
) {
	this.value = value?.toString() ?: ""
	if (defaultValue != null)
		placeholder = defaultValue.toString()
	onChange = {
		val target = it.target
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
