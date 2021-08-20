package formulaide.ui.fields

import formulaide.api.fields.DataField
import formulaide.api.fields.ShallowFormField
import formulaide.ui.components.styledField
import formulaide.ui.components.styledInput
import kotlinx.html.InputType
import kotlinx.html.js.onChangeFunction
import org.w3c.dom.HTMLInputElement
import react.fc

val NameEditor = fc<EditableFieldProps> { props ->
	val field = props.field
	val allowModification = field is DataField || field is ShallowFormField

	if (allowModification) {
		styledField("item-name-${field.id}", "Nom") {
			styledInput(InputType.text, "item-name-${field.id}", required = true) {
				placeholder = "Nom du champ"
				value = field.name
				onChangeFunction = {
					props.replace(
						props.field.requestCopy(name = (it.target as HTMLInputElement).value))
				}
			}
		}
	}
}
