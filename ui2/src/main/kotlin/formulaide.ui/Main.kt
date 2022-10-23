package formulaide.ui

import formulaide.ui.navigation.Navigation
import formulaide.ui.navigation.SelectProductionOrTest
import formulaide.ui.navigation.loadNavigation
import formulaide.ui.theme.ApplyTheme
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

	renderComposable(rootElementId = "root") {
		SelectProductionOrTest()
		ApplyTheme {
			Navigation()
		}
	}
}
