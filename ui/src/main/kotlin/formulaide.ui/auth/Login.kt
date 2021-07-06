package formulaide.ui.auth

import formulaide.api.users.PasswordLogin
import formulaide.client.Client
import formulaide.client.routes.login
import formulaide.ui.ScreenProps
import formulaide.ui.components.styledCard
import formulaide.ui.components.styledField
import formulaide.ui.components.styledFormCard
import formulaide.ui.components.styledInput
import formulaide.ui.defaultClient
import formulaide.ui.launchAndReportExceptions
import kotlinx.html.InputType
import kotlinx.html.js.onSubmitFunction
import org.w3c.dom.HTMLInputElement
import react.child
import react.functionalComponent
import react.useRef

external interface LoginProps : ScreenProps {
	var onLogin: (Client) -> Unit
}

/**
 * A login widget that requests an email and a password, and then updates the application's [client][LoginProps.client] by connecting to the server.
 *
 * @see Client
 * @see login
 */
val Login = functionalComponent<LoginProps> { props ->
	val email = useRef<HTMLInputElement>(null)
	val password = useRef<HTMLInputElement>(null)

	styledFormCard(
		"Espace employé",
		"Connectez-vous pour avoir accès à l'espace réservé aux employés.",
		"Se connecter",
		contents = {
			styledField("login-email", "Email") {
				styledInput(InputType.email, "login-email", required = true, ref = email)
			}

			styledField("login-password", "Mot de passe") {
				styledInput(InputType.password, "login-password", required = true, ref = password)
			}
		}
	) {
		onSubmitFunction = {
			it.preventDefault()

			launchAndReportExceptions(props) {
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

val LoginAccess = functionalComponent<ScreenProps> { props ->
	if (props.user == null) {
		child(Login) {
			attrs {
				client = props.client
				onLogin = props.connect
				scope = props.scope
				reportError = props.reportError
			}
		}
	} else {
		styledCard(
			"Espace employé",
			null,
			"Se déconnecter" to { props.connect(defaultClient) }
		) {}
	}
}
