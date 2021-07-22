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
import formulaide.ui.components2.useAsync
import formulaide.ui.utils.text
import kotlinx.html.InputType
import kotlinx.html.js.onSubmitFunction
import org.w3c.dom.HTMLInputElement
import react.RProps
import react.child
import react.fc
import react.useRef

/**
 * A login widget that requests an email and a password, and then updates the application's [client][client] by connecting to the server.
 *
 * @see Client
 * @see login
 */
val Login = fc<RProps> { _ ->
	val email = useRef<HTMLInputElement>(null)
	val password = useRef<HTMLInputElement>(null)

	val scope = useAsync()
	var client by useClient()

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

			scope.reportExceptions {
				val credentials = PasswordLogin(
					email = email.current?.value ?: error("Email manquant"),
					password = password.current?.value ?: error("Mot de passe manquant")
				)

				val token = client.login(credentials).token

				client = Client.Authenticated.connect(
					client.hostUrl,
					token
				)
			}
		}
	}
}

@Suppress("FunctionName")
fun PasswordModification(user: Email, previousScreen: Screen) = fc<RProps> {
	val oldPassword = useRef<HTMLInputElement>()
	val newPassword1 = useRef<HTMLInputElement>()
	val newPassword2 = useRef<HTMLInputElement>()

	val (client, connect) = useClient()
	val (me) = useUser()
	val (_, navigateTo) = useNavigation()
	val scope = useAsync()

	if (me == null) {
		styledCard("Modifier le mot de passe") {
			text("Chargement de l'utilisateur…")
		}
		return@fc
	}

	if (client !is Client.Authenticated) {
		styledCard("Modifier le mot de passe", failed = true) {
			text("Impossible de modifier le mot de passe sans être connecté")
		}
		return@fc
	}

	styledFormCard(
		"Modifier le mot de passe du compte ${user.email}",
		"Par sécurité, modifier le mot de passe va déconnecter tous vos appareils connectés.",
		"Modifier le mot de passe",
		contents = {
			styledField("old-password", "Mot de passe actuel") {
				styledInput(InputType.password,
				            "old-password",
				            ref = oldPassword,
				            required = !me.administrator)
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

			scope.reportExceptions {
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

				client.editPassword(request)

				if (user == me.email)
					connect(defaultClient)
				navigateTo(previousScreen)
			}
		}
	}
}

val LoginAccess = fc<RProps> {
	val (user) = useUser()
	val (client, connect) = useClient()
	val scope = useAsync()
	val (_, navigateTo) = useNavigation()

	if (user == null) {
		child(Login)
	} else {
		styledCard(
			"Espace employé",
			null,
			"Déconnexion" to {
				scope.reportExceptions {
					if (client is Client.Authenticated) client.logout()

					connect(defaultClient)
				}
			},
			"Modifier mon mot de passe" to {
				navigateTo(Screen.EditPassword(user.email,
				                               Screen.Home))
			}
		) {}
	}
}
