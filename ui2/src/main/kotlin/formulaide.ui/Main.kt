package formulaide.ui

import formulaide.ui.navigation.Navigation
import org.jetbrains.compose.web.renderComposable

fun main() {
	renderComposable(rootElementId = "root") {
		Navigation()
	}
}
