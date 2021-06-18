package formulaide.ui.auth

import formulaide.api.users.PasswordLogin
import formulaide.api.users.User
import formulaide.client.Client
import formulaide.client.routes.getMe
import formulaide.client.routes.login
import formulaide.ui.utils.text
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.html.InputType
import kotlinx.html.id
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onSubmitFunction
import org.w3c.dom.HTMLInputElement
import react.*
import react.dom.*

external interface LoginProps : RProps {
	var client: Client
	var onLogin: (Client, User) -> Unit
	var scope: CoroutineScope
}

/**
 * A login widget that requests an email and a password, and then updates the application's [client][LoginProps.client] by connecting to the server.
 *
 * @see Client
 * @see login
 */
@JsExport
val Login = functionalComponent<LoginProps> { props ->
	text("Bienvenue sur Formulaide, veuillez vous connecter pour accéder aux pages réservées aux employés.")

	val (email, setEmail) = useState<String?>(null)
	val (password, setPassword) = useState<String?>(null)

	form {
		label { text("Email") }
		input(InputType.email, name = "email") {
			key = "itemEmail"

			attrs {
				id = "login-email"
				autoFocus = true
				required = true
				placeholder = "Votre adresse email"
				onChangeFunction = { setEmail((it.target as HTMLInputElement).value) }
			}
		}

		br {}
		label { text("Mot de passe") }
		input(InputType.password, name = "password") {
			key = "itemPassword"

			attrs {
				id = "login-password"
				placeholder = "Votre mot de passe"
				required = true
				onChangeFunction = { setPassword((it.target as HTMLInputElement).value) }
			}
		}

		br {}
		input(InputType.submit, name = "login") {
			key = "itemLogin"

			attrs {
				id = "login-button"
			}
		}

		attrs {
			onSubmitFunction = {
				it.preventDefault()

				props.scope.launch {
					val credentials = PasswordLogin(
						email = email ?: error("Email manquant"),
						password = password ?: error("Mot de passe manquant")
					)

					val token = props.client.login(credentials).token

					val newClient = Client.Authenticated.connect(
						props.client.hostUrl,
						token
					)

					props.onLogin(newClient, newClient.getMe())
				}
			}
		}
	}
}

fun RBuilder.login(handler: LoginProps.() -> Unit) = child(Login) {
	attrs {
		handler()
	}
}
