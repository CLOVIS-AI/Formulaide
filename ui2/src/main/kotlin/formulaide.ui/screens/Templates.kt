package formulaide.ui.screens

import formulaide.core.User
import formulaide.ui.components.Page
import formulaide.ui.navigation.Screen

val TemplateList: Screen = Screen(
	title = "Modèles",
	requiredRole = User.Role.EMPLOYEE,
	"?templates",
	icon = "ri-clipboard-line",
	iconSelected = "ri-clipboard-fill"
) {
	Page(
		"Modèles"
	) {
		//TODO in future commits of this MR
	}
}
