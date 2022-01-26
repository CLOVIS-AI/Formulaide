package formulaide.ui.screens.homepage

import formulaide.ui.Screen
import formulaide.ui.components.cards.Card
import formulaide.ui.components.cards.action
import formulaide.ui.logout
import formulaide.ui.navigateTo
import formulaide.ui.useUser
import kotlinx.browser.window
import react.FC
import react.Props

val Homepage = FC<Props>("Homepage") {
	val (user) = useUser()

	if (user == null) {
		Login()
	} else {
		Card {
			title = "Espace employé"

			action("Déconnexion") { logout() }
			action("Modifier mon mot de passe") { navigateTo(Screen.EditPassword(user.email, Screen.Home)) }
			action("Aide") { window.open("https://clovis-ai.gitlab.io/formulaide/docs/user-guide.pdf") }

			TodoList()
		}
	}
}
