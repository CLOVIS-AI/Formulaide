package formulaide.ui

import formulaide.ui.navigation.Navigation
import formulaide.ui.navigation.loadNavigation
import formulaide.ui.theme.ApplyTheme
import org.jetbrains.compose.web.renderComposable

fun main() {
	js("require('./styles.css')")
	js("require('remixicon/fonts/remixicon.css')")

	loadNavigation()

	renderComposable(rootElementId = "root") {
		ApplyTheme {
			Navigation()
		}
	}
}
