package formulaide.ui

import kotlinx.browser.document
import react.dom.h1
import react.dom.render

val helloWorld get() = "Hello World!"

fun main() {
	render(document.getElementById("root")) {
		h1 {
			+helloWorld
		}
	}
}
