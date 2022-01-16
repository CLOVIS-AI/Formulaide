package formulaide.ui.fields.editors

import formulaide.api.fields.DataField
import formulaide.api.fields.ShallowFormField
import formulaide.ui.components.inputs.Input
import react.FC
import react.dom.html.InputType
import formulaide.ui.components.inputs.Field as UIField

val NameEditor = FC<EditableFieldProps>("NameEditor") { props ->
	val field = props.field
	val allowModification = field is DataField || field is ShallowFormField

	if (allowModification) {
		val id = "item-name-${props.uniqueId}"

		UIField {
			this.id = id
			text = "Nom"

			Input {
				type = InputType.text
				this.id = id
				required = true
				placeholder = "Nom du champ"
				value = field.name
				onChange = { props.replace(props.field.requestCopy(name = it.target.value)) }
			}
		}
	}
}
