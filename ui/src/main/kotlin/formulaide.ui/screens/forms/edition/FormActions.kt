package formulaide.ui.screens.forms.edition

import formulaide.api.data.Action
import formulaide.api.types.Ref.Companion.createRef
import formulaide.ui.components.StyledButton
import formulaide.ui.components.inputs.Field
import formulaide.ui.components.text.ErrorText
import formulaide.ui.useServices
import formulaide.ui.utils.replace
import react.FC
import react.Props
import react.useMemo

external interface FormActionsProps : Props {
	var actions: List<Action>
	var updateActions: (List<Action>.() -> List<Action>) -> Unit
}

val FormActions = FC<FormActionsProps>("FormActions") { props ->
	val actions = props.actions
	val services by useServices()

	val maxActionId = useMemo(actions) { actions.maxOfOrNull { it.id.toInt() }?.plus(1) ?: 0 }
	val maxActionFieldId = useMemo(actions) {
		actions.flatMap { it.fields?.fields ?: emptyList() }.maxOfOrNull { it.id.toInt() }
			?.plus(1)
			?: 0
	}

	Field {
		id = "new-form-actions"
		text = "Étapes"

		for ((i, action) in actions.sortedBy { it.order }.withIndex()) {
			FormAction {
				+props

				this.index = i
				this.action = action
				this.onReplace = { props.updateActions { replace(i, it) } }
				this.maxFieldId = maxActionFieldId
			}
		}

		StyledButton {
			text = "Ajouter une étape"
			action = {
				props.updateActions {
					this + Action(
						id = maxActionId.toString(),
						order = size,
						services.getOrNull(0)?.createRef()
							?: error("Aucun service n'a été trouvé"),
						name = "",
					)
				}
			}
		}
		if (actions.isEmpty())
			ErrorText { text = "Un formulaire doit avoir au moins une étape." }
	}
}
