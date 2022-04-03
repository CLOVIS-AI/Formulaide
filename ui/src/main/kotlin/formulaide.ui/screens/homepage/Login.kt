package formulaide.ui.screens.homepage

import formulaide.api.users.PasswordLogin
import formulaide.client.Client
import formulaide.client.routes.login
import formulaide.ui.components.cards.FormCard
import formulaide.ui.components.cards.submit
import formulaide.ui.components.inputs.Field
import formulaide.ui.components.inputs.Input
import formulaide.ui.useClient
import formulaide.ui.utils.traceRenders
import react.FC
import react.Props
import react.dom.html.InputType
import react.useState

/**
 * A login widget that requests an email and a password, and then updates the application's [Client] by connecting to the server.
 *
 * @see login
 */
val Login = FC<Props>("Login") {
	traceRenders("Login")

	var email by useState("")
	var password by useState("")

	var client by useClient("Login")

	FormCard {
		title = "Espace employé"
		subtitle = "Connectez-vous pour avoir accès à l'espace réservé aux employés"

		submit("Se connecter") {
			require(email.isNotBlank()) { "Email manquant" }
			require(password.isNotBlank()) { "Mot de passe manquant" }

			val credentials = PasswordLogin(
				email = email,
				password = password
			)

			launch {
				val token = client.login(credentials).token

				client = Client.Authenticated.connect(
					client.hostUrl,
					token
				)
			}
		}

		Field {
			id = "login-email"
			text = "Email"

			Input {
				type = InputType.email
				id = "login-email"
				required = true
				value = email
				onChange = { email = it.target.value }
			}
		}

		Field {
			id = "login-password"
			text = "Mot de passe"

			Input {
				type = InputType.password
				id = "login-password"
				required = true
				value = password
				onChange = { password = it.target.value }
			}
		}
	}
}
