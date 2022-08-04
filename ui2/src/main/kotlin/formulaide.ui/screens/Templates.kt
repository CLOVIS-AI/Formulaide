package formulaide.ui.screens

import formulaide.ui.navigation.Screen
import formulaide.ui.utils.Role
import org.jetbrains.compose.web.dom.Article
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text

val TemplateList: Screen = Screen(
	title = "Modèles",
	requiredRole = Role.EMPLOYEE,
	"?templates",
	icon = "ri-clipboard-line",
	iconSelected = "ri-clipboard-fill"
) {
	Article {
		P {
			Text("Modèles")
		}
	}
}
