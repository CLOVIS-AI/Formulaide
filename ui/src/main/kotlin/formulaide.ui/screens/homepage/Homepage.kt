package formulaide.ui.screens.homepage

import formulaide.api.data.helpUrlOrDefault
import formulaide.ui.*
import formulaide.ui.components.cards.Card
import formulaide.ui.components.cards.action
import kotlinx.browser.window
import react.FC
import react.Props

val Homepage = FC<Props>("Homepage") {
	val (user) = useUser()
	val config by useConfig()

	if (user == null) {
		Login()
	} else {
		if (user.administrator) {
			Alerts()
		}

		Card {
			title = "Espace employé"

			action("Déconnexion") { logout() }
			action("Modifier mon mot de passe") { navigateTo(Screen.EditPassword(user.email, Screen.Home)) }
			action("Aide") { window.open(config.helpUrlOrDefault) }

			TodoList()
		}
	}
}
