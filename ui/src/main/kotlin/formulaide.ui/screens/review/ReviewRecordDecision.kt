package formulaide.ui.screens.review

import formulaide.api.data.RecordState
import formulaide.api.types.Ref.Companion.createRef
import formulaide.ui.components.StyledButton
import formulaide.ui.components.inputs.Field
import formulaide.ui.components.inputs.Input
import formulaide.ui.utils.DelegatedProperty.Companion.DelegatedProperty
import react.FC
import react.dom.html.InputType
import react.dom.html.ReactHTML.div

private enum class ReviewDecision {
	PREVIOUS,
	NO_CHANGE,
	NEXT,
	REFUSE,
}

internal external interface ReviewRecordDecisionProps : ReviewRecordVariantProps {
	var nextAction: RecordState.Action?

	var selectedDestination: RecordState
	var updateSelectedDestination: ((RecordState).() -> RecordState) -> Unit

	var reason: String?
	var updateReason: (String?.() -> String?) -> Unit
}

internal val ReviewRecordDecision = FC<ReviewRecordDecisionProps>("ReviewRecordDecision") { props ->
	var selectedDestination by DelegatedProperty({ props.selectedDestination }, props.updateSelectedDestination)
	var reason by DelegatedProperty({ props.reason }, props.updateReason)

	val state = props.record.state
	val nextAction = props.nextAction

	val decision = when {
		selectedDestination is RecordState.Action && selectedDestination == state -> ReviewDecision.NO_CHANGE
		selectedDestination is RecordState.Refused && state is RecordState.Refused -> ReviewDecision.NO_CHANGE
		selectedDestination is RecordState.Refused -> ReviewDecision.REFUSE
		selectedDestination is RecordState.Action && selectedDestination == nextAction -> ReviewDecision.NEXT
		else -> ReviewDecision.PREVIOUS
	}

	div {
		className = "print:hidden"

		div {
			+"Votre décision :"

			if ((state as? RecordState.Action)?.current?.obj != props.form.actions.firstOrNull())
				StyledButton {
					text = "Renvoyer à une étape précédente"
					enabled = decision != ReviewDecision.PREVIOUS
					action = { RecordState.Action(props.form.actions.first().createRef()) }
				}

			StyledButton {
				text = "Conserver"
				enabled = decision != ReviewDecision.NO_CHANGE
				action = { selectedDestination = state; reason = null }
			}

			if (nextAction != null)
				StyledButton {
					text = "Accepter"
					enabled = decision != ReviewDecision.NEXT
					action = { selectedDestination = nextAction }
				}

			if (state != RecordState.Refused)
				StyledButton {
					text = "Refuser"
					enabled = decision != ReviewDecision.REFUSE
					action = { selectedDestination = RecordState.Refused }
				}
		}

		if (decision == ReviewDecision.PREVIOUS) div {
			+"Étapes précédentes :"

			for (previousState in props.form.actions.map { RecordState.Action(it.createRef()) }) {
				if (previousState == state)
					break

				StyledButton {
					text = previousState.current.obj.name
					enabled = selectedDestination != previousState
					action = { selectedDestination = previousState }
				}
			}
		}

		if (decision != ReviewDecision.NEXT) Field {
			id = "record-${props.record.id}-reason"
			text = "Pourquoi ce choix ?"

			Input {
				type = InputType.text
				id = "record-${props.record.id}-reason"
				required = decision == ReviewDecision.REFUSE
				value = reason ?: ""
				onChange = { reason = it.target.value }
			}
		}
	}
}
