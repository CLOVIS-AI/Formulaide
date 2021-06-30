package formulaide.ui

import formulaide.ui.utils.detectTests
import kotlinext.js.require
import kotlinx.browser.document
import react.child
import react.dom.render

fun main() {
	if (detectTests())
		return

	require("./styles.css")

	render(document.getElementById("root")) {
		child(App) {}
	}
}
