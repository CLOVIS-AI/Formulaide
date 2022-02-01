package formulaide.ui.components.cards

import formulaide.ui.components.StyledButton
import react.FC
import react.PropsWithChildren
import react.dom.html.ReactHTML.div

external interface CardProps : PropsWithChildren, CardTitleProps {
	var id: String?
	var actions: List<Pair<String, suspend () -> Unit>>?
	var failed: Boolean?
}

/**
 * Adds an action to the list of [CardProps.actions].
 */
fun CardProps.action(text: String, block: suspend () -> Unit) {
	actions = (actions ?: emptyList()) + (text to block)
}

val Card = FC<CardProps>("Card") { props ->
	val actions = props.actions ?: emptyList()

	CardShell {
		this.id = props.id
		this.failed = props.failed

		CardTitle { +props }

		div {
			className = "pt-4"
			props.children()
		}

		if (actions.isNotEmpty()) div {
			className = "pt-4"

			for ((text, block) in actions) {
				StyledButton {
					this.text = text
					this.emphasize = text === actions.first().first
					this.action = block
				}
			}
		}
	}
}
