package formulaide.ui.components

import formulaide.ui.utils.text
import kotlinx.html.DIV
import react.RBuilder
import react.dom.RDOMBuilder
import react.dom.div
import react.dom.label

fun RBuilder.styledNesting(displayName: String, block: RDOMBuilder<DIV>.() -> Unit) {
	label("block") { text(displayName) }
	div("p-4 pl-8 pr-8") {
		block()
	}
}
