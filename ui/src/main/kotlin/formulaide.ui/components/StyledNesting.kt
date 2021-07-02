package formulaide.ui.components

import kotlinx.html.DIV
import react.RBuilder
import react.dom.RDOMBuilder
import react.dom.div

fun RBuilder.styledNesting(block: RDOMBuilder<DIV>.() -> Unit) {
	div("p-2 pl-4 pr-4") {
		block()
	}
}
