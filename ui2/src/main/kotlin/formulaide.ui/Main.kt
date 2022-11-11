package formulaide.ui

import formulaide.ui.navigation.Navigation
import formulaide.ui.navigation.SelectProductionOrTest
import formulaide.ui.navigation.loadNavigation
import formulaide.ui.theme.ApplyTheme
import kotlinx.browser.document
import org.jetbrains.compose.web.renderComposable

fun main() {
	js("require('./styles.css')")
	js("require('remixicon/fonts/remixicon.css')")

	// Language=JavaScript
	js(
		"""
		window.originalFetch = window.fetch;
  		window.fetch = function (resource, init) {
      		return window.originalFetch(resource, Object.assign({ credentials: 'include' }, init || {}));
  		};
	"""
	)

	loadNavigation()

	// By default, Formulaide displays a 'init-loading' div with a loading message
	// We must delete them when we are ready to display the real UI
	val initLoading = document.querySelector("#init-loading")
	initLoading?.parentNode?.removeChild(initLoading)

	renderComposable(rootElementId = "root") {
		SelectProductionOrTest {
			ApplyTheme {
				Navigation()
			}
		}
	}
}
