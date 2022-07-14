package formulaide.ui

import formulaide.ui.utils.detectTests
import kotlinext.js.require
import kotlinx.browser.document
import react.createElement
import react.dom.render

fun main() {
	if (detectTests())
		return

	require("./styles.css")

	val app = createElement(StyledAppFrame)

	render(app, document.getElementById("root") ?: error("Could not find the 'root' element.")) {}
}
