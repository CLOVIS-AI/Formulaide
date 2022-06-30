package formulaide.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import formulaide.ui.screens.DummyScreen
import formulaide.ui.screens.Home
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.url.URL

@Suppress("ObjectPropertyName")
private var _currentScreen by mutableStateOf(Home)

var currentScreen: Screen
	get() = _currentScreen
	set(value) {
		window.history.pushState(null, value.title, value.route)
		document.title = "${value.title} • Formulaide"
		_currentScreen = value
	}

val screens = listOf(
	Home,
	DummyScreen,
)

@Composable
fun Navigation() {
	_currentScreen()
}

fun loadNavigation() {
	val url = URL(window.location.href)
		.search

	val params = url.split("&")
	console.log("Loading the navigation system... Requested route: $params")

	val target = params.getOrNull(0) ?: Home.route
	val screen = screens
		.firstOrNull { it.route.startsWith(target) }
	console.log("...it refers to the screen $screen")

	if (screen != null)
		currentScreen = screen
}
