package formulaide.ui.screens.forms.edition

import formulaide.ui.components.inputs.Field
import formulaide.ui.components.inputs.Input
import react.FC
import react.dom.html.InputType

val FormActionTitle = FC<FormActionProps>("FormActionTitle") { props ->
	val action = props.action

	Field {
		id = "new-form-action-${action.id}-name"
		text = "Nom de l'étape"

		Input {
			type = InputType.text
			id = "new-form-action-${action.id}-name"
			required = true
			value = action.name
			onChange = { props.onReplace(action.copy(name = it.target.value)) }
		}
	}
}
