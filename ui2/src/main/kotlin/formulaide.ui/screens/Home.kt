package formulaide.ui.screens

import formulaide.ui.components.Page
import formulaide.ui.navigation.Screen
import formulaide.ui.utils.Role

val Home: Screen = Screen(
	"Accueil",
	Role.ANONYMOUS,
	"?home",
	icon = "ri-home-line",
	iconSelected = "ri-home-fill",
) {
	Page(
		"Formulaide"
	) {}
}
