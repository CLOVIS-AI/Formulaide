package formulaide.ui.screens

import formulaide.ui.navigation.Screen
import formulaide.ui.navigation.currentScreen
import formulaide.ui.utils.Role
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text

val DummyScreen: Screen = Screen(
	title = "Test",
	requiredRole = Role.ANONYMOUS,
	"?dummy"
) {
	P {
		Text("Écran de test")
	}

	Button({
		       onClick { currentScreen = Home }
	       }) {
		Text("Aller à l'accueil")
	}
}
