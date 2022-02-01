package formulaide.ui.screens.homepage

import formulaide.api.types.Email
import formulaide.api.users.PasswordEdit
import formulaide.client.Client
import formulaide.client.routes.editPassword
import formulaide.ui.*
import formulaide.ui.components.cards.Card
import formulaide.ui.components.cards.FormCard
import formulaide.ui.components.cards.submit
import formulaide.ui.components.inputs.Field
import formulaide.ui.components.inputs.Input
import react.FC
import react.Props
import react.dom.html.InputType
import react.useState

@Suppress("FunctionName")
fun PasswordEditor(user: Email, previousScreen: Screen) = FC<Props>("PasswordEditor") {
	traceRenders("PasswordModification")

	var oldPassword by useState<String>()
	var newPassword1 by useState<String>()
	var newPassword2 by useState<String>()

	val (client, connect) = useClient()
	val (me) = useUser()

	if (me == null) {
		Card {
			title = "Modifier le mot de passe"
			+"Chargement de l'utilisateur…"
		}
		return@FC
	}

	if (client !is Client.Authenticated) {
		Card {
			title = "Modifier le mot de passe"
			+"impossible de modifier le mot de passe sans être connecté"
		}
		return@FC
	}

	FormCard {
		title = "Modifier le mot de passe du compte ${user.email}"
		subtitle = "Par sécurité, modifier le mot de passe va déconnecter tous vos appareils."

		submit("Modifier le mot de passe") {
			require(newPassword1 == newPassword2) { "Le nouveau mot de passe et sa confirmation ne sont pas identiques" }

			val request = PasswordEdit(
				user,
				oldPassword,
				newPassword1 ?: error("Le nouveau mot de passe n'a pas été rempli")
			)

			launch {
				client.editPassword(request)

				if (user == me.email)
					connect { defaultClient }
				navigateTo(previousScreen)
			}
		}

		Field {
			id = "old-password"
			text = "Mot de passe actuel"

			Input {
				type = InputType.password
				id = "old-password"
				value = oldPassword
				required = !me.administrator
				onChange = { oldPassword = it.target.value }
			}
		}

		Field {
			id = "new-password-1"
			text = "Nouveau mot de passe"

			Input {
				type = InputType.password
				id = "new-password-1"
				required = true
				value = newPassword1
				onChange = { newPassword1 = it.target.value }
			}
		}

		Field {
			id = "new-password-2"
			text = "Confirmer le nouveau mot de passe"

			Input {
				type = InputType.password
				id = "new-password-2"
				required = true
				value = newPassword2
				onChange = { newPassword2 = it.target.value }
			}
		}
	}
}
