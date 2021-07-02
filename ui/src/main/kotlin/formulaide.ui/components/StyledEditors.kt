package formulaide.ui.components

import formulaide.ui.utils.text
import react.RBuilder
import react.dom.div
import react.dom.p

fun RBuilder.styledFieldEditorShell(
	displayName: String,
	contents: RBuilder.() -> Unit,
) {
	div("my-4") {
		p("text-lg") { text(displayName) }

		contents()
	}
}
