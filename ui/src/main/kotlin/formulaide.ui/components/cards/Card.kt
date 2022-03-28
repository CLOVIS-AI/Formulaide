package formulaide.ui.components.cards

import formulaide.ui.components.StyledButton
import formulaide.ui.utils.classes
import react.FC
import react.PropsWithChildren
import react.dom.html.ReactHTML.div

external interface CardProps : PropsWithChildren, CardTitleProps, CommonCardProps {
	var actions: List<Pair<String, suspend () -> Unit>>?
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

		Header {
			CardTitle { +props }

			props.header?.invoke(this)
		}

		div {
			classes = "pt-4"
			+props.children
		}

		Footer {
			props.footer?.invoke(this)

			if (actions.isNotEmpty()) div {
				classes = "pt-4"

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
}
