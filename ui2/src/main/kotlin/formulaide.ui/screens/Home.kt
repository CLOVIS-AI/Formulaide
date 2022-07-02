package formulaide.ui.screens

import formulaide.ui.navigation.Screen
import formulaide.ui.navigation.currentScreen
import formulaide.ui.utils.Role
import org.jetbrains.compose.web.dom.Article
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text

val Home: Screen = Screen(
	"Accueil",
	Role.ANONYMOUS,
	"?home",
	icon = "ri-home-line",
	iconSelected = "ri-home-fill",
) {
	Article {
		P {
			Text("Formulaide")
		}

		Button({
			       onClick { currentScreen = DummyScreen }
		       }) {
			Text("Aller sur l'écran de test")
		}
	}
}
