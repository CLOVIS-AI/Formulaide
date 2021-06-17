package formulaide.ui

import formulaide.ui.utils.detectTests
import kotlinx.browser.document
import react.dom.render

fun main() {
	if (detectTests())
		return

	render(document.getElementById("root")) {
		h1 {
			+helloWorld
		}
	}
}
