package formulaide.ui.fields

import formulaide.api.fields.DataField
import formulaide.api.fields.ShallowFormField
import formulaide.ui.components.styledField
import formulaide.ui.components.styledInput
import react.FC
import react.dom.html.InputType

val NameEditor = FC<EditableFieldProps>("NameEditor") { props ->
	val field = props.field
	val allowModification = field is DataField || field is ShallowFormField

	if (allowModification) {
		val id = "item-name-${props.uniqueId}"

		styledField(id, "Nom") {
			styledInput(InputType.text, id, required = true) {
				placeholder = "Nom du champ"
				value = field.name
				onChange = {
					props.replace(
						props.field.requestCopy(name = it.target.value))
				}
			}
		}
	}
}
