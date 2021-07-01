package formulaide.ui.components

import formulaide.ui.utils.text
import kotlinx.html.DIV
import react.RBuilder
import react.dom.*

fun RBuilder.styledNesting(displayName: String, block: RDOMBuilder<DIV>.() -> Unit) {
	br {}
	label { text(displayName) }
	div {
		attrs {
			jsStyle {
				marginLeft = "2rem"
			}
		}

		block()
	}
}
