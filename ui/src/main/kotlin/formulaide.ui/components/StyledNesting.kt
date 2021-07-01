package formulaide.ui.components

import kotlinx.html.DIV
import react.RBuilder
import react.dom.RDOMBuilder
import react.dom.attrs
import react.dom.div
import react.dom.jsStyle

fun RBuilder.styledNesting(block: RDOMBuilder<DIV>.() -> Unit) {
	div {
		attrs {
			jsStyle {
				marginLeft = "2rem"
			}
		}

		block()
	}
}
