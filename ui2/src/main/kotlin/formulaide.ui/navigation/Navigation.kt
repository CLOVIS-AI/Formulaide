package formulaide.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import formulaide.ui.screens.Home
import kotlinx.browser.document
import kotlinx.browser.window

private var _currentScreen by mutableStateOf(Home)

var currentScreen: Screen
	get() = _currentScreen
	set(value) {
		window.history.pushState(null, value.title, value.route)
		document.title = "${value.title} • Formulaide"
		_currentScreen = value
	}

@Composable
fun Navigation() {
	_currentScreen()
}
