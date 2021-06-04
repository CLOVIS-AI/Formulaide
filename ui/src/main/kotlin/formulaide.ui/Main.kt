package formulaide.ui

import formulaide.ui.utils.detectTests
import kotlinx.browser.document
import react.dom.h1
import react.dom.render

val helloWorld get() = "Hello World!"

fun main() {
	if (detectTests())
		return

	render(document.getElementById("root")) {
		h1 {
			+helloWorld
		}
	}
}
