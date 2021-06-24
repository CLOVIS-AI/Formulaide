package formulaide.ui.auth

import formulaide.api.users.PasswordLogin
import formulaide.client.Client
import formulaide.client.routes.login
import formulaide.ui.ScreenProps
import formulaide.ui.defaultClient
import formulaide.ui.utils.text
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.html.InputType
import kotlinx.html.id
import kotlinx.html.js.onClickFunction
import kotlinx.html.js.onSubmitFunction
import org.w3c.dom.HTMLInputElement
import react.RProps
import react.child
import react.dom.*
import react.functionalComponent
import react.useRef

external interface LoginProps : RProps {
	var client: Client
	var onLogin: (Client) -> Unit
	var scope: CoroutineScope
}

/**
 * A login widget that requests an email and a password, and then updates the application's [client][LoginProps.client] by connecting to the server.
 *
 * @see Client
 * @see login
 */
val Login = functionalComponent<LoginProps> { props ->
	text("Bienvenue sur Formulaide, veuillez vous connecter pour accéder aux pages réservées aux employés.")

	val email = useRef<HTMLInputElement>(null)
	val password = useRef<HTMLInputElement>(null)

	form {
		label { text("Email") }
		input(InputType.email, name = "email") {
			key = "itemEmail"

			attrs {
				id = "login-email"
				autoFocus = true
				required = true
				placeholder = "Votre adresse email"
				ref = email
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
				ref = password
			}
		}

		br {}
		input(InputType.submit, name = "login") {
			key = "itemLogin"

			attrs {
				id = "login-button"
				value = "Se connecter"
			}
		}

		attrs {
			onSubmitFunction = {
				it.preventDefault()

				props.scope.launch {
					val credentials = PasswordLogin(
						email = email.current?.value ?: error("Email manquant"),
						password = password.current?.value ?: error("Mot de passe manquant")
					)

					val token = props.client.login(credentials).token

					val newClient = Client.Authenticated.connect(
						props.client.hostUrl,
						token
					)

					props.onLogin(newClient)
				}
			}
		}
	}
}

val LoginAccess = functionalComponent<ScreenProps> { props ->
	if (props.user == null) {
		child(Login) {
			attrs {
				client = props.client
				onLogin = props.connect
				scope = props.scope
			}
		}
	} else {
		button {
			text("Se déconnecter")
			attrs {
				onClickFunction = { props.connect(defaultClient) }
			}
		}
	}
}
