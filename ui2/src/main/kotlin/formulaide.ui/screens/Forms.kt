package formulaide.ui.screens

import formulaide.ui.components.Page
import formulaide.ui.navigation.Screen
import formulaide.ui.utils.Role

val FormList: Screen = Screen(
	title = "Formulaires",
	requiredRole = Role.ANONYMOUS,
	"?forms",
	icon = "ri-survey-line",
	iconSelected = "ri-survey-fill"
) {
	Page(
		"Formulaires"
	) {
		//TODO in future commits of this MR
	}
}
