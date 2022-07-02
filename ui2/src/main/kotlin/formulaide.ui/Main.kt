package formulaide.ui

import formulaide.ui.navigation.Navigation
import formulaide.ui.navigation.loadNavigation
import org.jetbrains.compose.web.renderComposable

fun main() {
	js("require('remixicon/fonts/remixicon.css')")

	loadNavigation()

	renderComposable(rootElementId = "root") {
		Navigation()
	}
}
