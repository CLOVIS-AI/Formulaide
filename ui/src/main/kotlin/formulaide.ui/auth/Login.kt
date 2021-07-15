package formulaide.ui.auth

import formulaide.api.types.Email
import formulaide.api.users.PasswordEdit
import formulaide.api.users.PasswordLogin
import formulaide.client.Client
import formulaide.client.routes.editPassword
import formulaide.client.routes.login
import formulaide.ui.*
import formulaide.ui.components.styledCard
import formulaide.ui.components.styledField
import formulaide.ui.components.styledFormCard
import formulaide.ui.components.styledInput
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

@Suppress("FunctionName")
fun PasswordModification(user: Email, previousScreen: Screen) =
	functionalComponent<ScreenProps> { props ->
		val oldPassword = useRef<HTMLInputElement>()
		val newPassword1 = useRef<HTMLInputElement>()
		val newPassword2 = useRef<HTMLInputElement>()

		styledFormCard(
			"Modifier le mot de passe du compte ${user.email}",
			"Par sécurité, modifier le mot de passe va déconnecter tous vos appareils connectés.",
			"Modifier le mot de passe",
			contents = {
				styledField("old-password", "Mot de passe actuel") {
					styledInput(InputType.password,
					            "old-password",
					            ref = oldPassword,
					            required = !(props.user?.administrator ?: false))
				}

				styledField("new-password-1", "Nouveau de mot de passe") {
					styledInput(InputType.password,
					            "new-password-1",
					            required = true,
					            ref = newPassword1)
				}

				styledField("new-password-2", "Confirmer le nouveau mot de passe") {
					styledInput(InputType.password,
					            "new-password-2",
					            required = true,
					            ref = newPassword2)
				}
			}
		) {
			onSubmitFunction = {
				it.preventDefault()

				reportExceptions(props) {
					val oldPasswordValue = oldPassword.current?.value
					val newPassword1Value = newPassword1.current?.value
					val newPassword2Value = newPassword2.current?.value

					requireNotNull(newPassword1Value) { "Le nouveau mot de passe n'a pas été rempli" }
					require(newPassword1Value == newPassword2Value) { "Le nouveau mot de passe et sa confirmation ne sont pas identiques" }

					val request = PasswordEdit(
						user,
						oldPasswordValue,
						newPassword1Value
					)

					launchAndReportExceptions(props) {
						val client = props.client
						require(client is Client.Authenticated) { "Impossible de modifier le mot de passe si on n'est pas connecté" }

						client.editPassword(request)

						if (user == props.user?.email)
							props.connect(defaultClient)
						props.navigateTo(previousScreen)
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
				reportError = props.reportError
			}
		}
	} else {
		styledCard(
			"Espace employé",
			null,
			"Déconnexion" to {
				launchAndReportExceptions(props) {
					val client = props.client
					if (client is Client.Authenticated) client.logout()

					props.connect(defaultClient)
				}
			},
			"Modifier mon mot de passe" to {
				props.navigateTo(Screen.EditPassword(props.user!!.email,
				                                     Screen.Home))
			}
		) {}
	}
}
