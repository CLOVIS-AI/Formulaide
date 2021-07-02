package formulaide.ui.fields

import formulaide.api.fields.DataField
import formulaide.api.fields.FormField
import formulaide.ui.components.styledField
import formulaide.ui.components.styledInput
import kotlinx.html.InputType
import kotlinx.html.js.onChangeFunction
import org.w3c.dom.HTMLInputElement
import react.functionalComponent

val NameEditor = functionalComponent<EditableFieldProps> { props ->
	val field = props.field
	val allowModification = field is DataField || field is FormField.Shallow

	if (allowModification) {
		styledField("item-name-${field.id}", "Nom") {
			styledInput(InputType.text, "item-name-${field.id}", required = true) {
				placeholder = "Nom du champ"
				onChangeFunction = {
					props.replace(
						props.field.set(name = (it.target as HTMLInputElement).value))
				}
			}
		}
	}
}
