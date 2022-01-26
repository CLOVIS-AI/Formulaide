package formulaide.ui.screens.forms.edition

import formulaide.api.data.Action
import formulaide.ui.components.inputs.Nesting
import formulaide.ui.utils.remove
import react.FC
import react.dom.html.ReactHTML.div
import react.key

external interface FormActionProps : FormActionsProps {
	var index: Int
	var action: Action

	var onReplace: (Action) -> Unit
	var maxFieldId: Int
}

val FormAction = FC<FormActionProps>("FormActionProps") { props ->
	val i = props.index
	val action = props.action

	div {
		key = action.id

		Nesting {
			depth = 0
			fieldNumber = i
			onDeletion = { props.updateActions { remove(i) } }

			FormActionTitle { +props }
			FormActionReviewer { +props }
			FormActionFields { +props }
		}
	}
}
