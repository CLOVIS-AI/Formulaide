package formulaide.ui.screens

import formulaide.ui.navigation.Screen
import formulaide.ui.utils.Role
import org.jetbrains.compose.web.dom.Article
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text

val FormList: Screen = Screen(
	title = "Formulaires",
	requiredRole = Role.ANONYMOUS,
	"?forms",
	icon = "ri-survey-line",
	iconSelected = "ri-survey-fill"
) {
	Article {
		P {
			Text("Formulaires")
		}
	}
}
