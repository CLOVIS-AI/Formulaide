package formulaide.ui.fields

import formulaide.api.fields.DataField
import formulaide.api.fields.FormField
import formulaide.ui.utils.text
import kotlinx.html.InputType
import kotlinx.html.js.onChangeFunction
import org.w3c.dom.HTMLInputElement
import react.dom.attrs
import react.dom.input
import react.dom.label
import react.functionalComponent

val NameEditor = functionalComponent<FieldProps2> { props ->
	val field = props.field
	val allowModification = field is DataField || field is FormField.Shallow

	if (allowModification) {
		label { text("Nom") }
		input(InputType.text, name = "item-name") {
			attrs {
				required = true
				placeholder = "Nom du champ"
				onChangeFunction = {
					props.replace(
						props.field.set(name = (it.target as HTMLInputElement).value))
				}
			}
		}
	} else {
		text(" ${props.field.name}")
	}
}
