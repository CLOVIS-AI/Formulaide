package formulaide.ui.fields

import formulaide.api.fields.Field
import formulaide.api.fields.SimpleField
import formulaide.ui.components.styledButton
import formulaide.ui.components.styledField
import formulaide.ui.components.styledInput
import kotlinx.html.INPUT
import kotlinx.html.InputType
import kotlinx.html.js.onChangeFunction
import org.w3c.dom.HTMLInputElement
import react.RBuilder
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
							           it.toIntOrNull()?.let { simple.copy(maxLength = it) }
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
						           update = { it.toLongOrNull()?.let { simple.copy(min = it) } })
					}
					cancelButton(simple.min, props, field, update = { simple.copy(min = 0) })
				}

				val id2 = idOf(field, "max")
				styledField(id2, "Valeur maximale") {
					styledInput(InputType.number, id2) {
						setHandler(simple.max,
						           props,
						           field,
						           update = { it.toLongOrNull()?.let { simple.copy(max = it) } })
					}
					cancelButton(simple.max, props, field, update = { simple.copy(max = 0) })
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
	update: (String) -> SimpleField?,
) {
	this.value = value?.toString() ?: ""
	this.onChangeFunction = {
		val target = it.target as HTMLInputElement
		val updated = update(target.value)
		if (updated != null)
			props.replace(field.requestCopy(updated))
	}
}
