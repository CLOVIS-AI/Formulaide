package formulaide.ui.screens

import formulaide.ui.components.Page
import formulaide.ui.navigation.Screen
import formulaide.ui.utils.Role

val TemplateList: Screen = Screen(
	title = "Modèles",
	requiredRole = Role.EMPLOYEE,
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
