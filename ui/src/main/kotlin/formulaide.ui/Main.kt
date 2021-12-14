package formulaide.ui

import formulaide.ui.components.styledFrame
import formulaide.ui.utils.detectTests
import kotlinext.js.require
import kotlinx.browser.document
import react.dom.render

fun main() {
	if (detectTests())
		return

	require("./styles.css")

	render(document.getElementById("root") ?: error("Could not find the 'root' element.")) {
		traceRenders("root")

		styledFrame {
			child(App)
		}
	}
}
