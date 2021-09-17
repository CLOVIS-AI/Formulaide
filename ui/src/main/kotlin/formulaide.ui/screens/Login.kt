package formulaide.ui.screens

import formulaide.api.data.Form
import formulaide.api.types.Email
import formulaide.api.users.PasswordEdit
import formulaide.api.users.PasswordLogin
import formulaide.client.Client
import formulaide.client.routes.editPassword
import formulaide.client.routes.login
import formulaide.client.routes.todoList
import formulaide.ui.*
import formulaide.ui.components.*
import formulaide.ui.utils.DelegatedProperty.Companion.asDelegated
import formulaide.ui.utils.text
import formulaide.ui.utils.useListEquality
import kotlinx.browser.window
import kotlinx.html.InputType
import org.w3c.dom.HTMLInputElement
import react.*
import react.dom.p

/**
 * A login widget that requests an email and a password, and then updates the application's [client][client] by connecting to the server.
 *
 * @see Client
 * @see login
 */
val Login = fc<Props> {
	traceRenders("Login")

	val email = useRef<HTMLInputElement>(null)
	val password = useRef<HTMLInputElement>(null)

	var client by useClient()

	styledFormCard(
		"Espace employé",
		"Connectez-vous pour avoir accès à l'espace réservé aux employés.",
		"Se connecter" to {
			val credentials = PasswordLogin(
				email = email.current?.value ?: error("Email manquant"),
				password = password.current?.value ?: error("Mot de passe manquant")
			)

			launch {
				val token = client.login(credentials).token

				client = Client.Authenticated.connect(
					client.hostUrl,
					token
				)
			}
		}
	) {
		styledField("login-email", "Email") {
			styledInput(InputType.email, "login-email", required = true, ref = email)
		}

		styledField("login-password", "Mot de passe") {
			styledInput(InputType.password, "login-password", required = true, ref = password)
		}
	}
}

@Suppress("FunctionName")
fun PasswordModification(user: Email, previousScreen: Screen) = fc<Props> {
	traceRenders("PasswordModification")

	val oldPassword = useRef<HTMLInputElement>()
	val newPassword1 = useRef<HTMLInputElement>()
	val newPassword2 = useRef<HTMLInputElement>()

	val (client, connect) = useClient()
	val (me) = useUser()

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
		"Modifier le mot de passe" to {
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

			launch {
				client.editPassword(request)

				if (user == me.email)
					connect { defaultClient }
				navigateTo(previousScreen)
			}
		}
	) {
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
}

val LoginAccess = fc<Props> {
	traceRenders("LoginAccess")

	val (user) = useUser()
	val scope = useAsync()

	if (user == null) {
		child(Login)
	} else {
		styledCard(
			"Espace employé",
			null,
			"Déconnexion" to {
				scope.reportExceptions {
					logout()
				}
			},
			"Modifier mon mot de passe" to {
				navigateTo(Screen.EditPassword(user.email,
				                               Screen.Home))
			},
			"Aide" to {
				window.open("https://arcachon-ville.gitlab.io/formulaide/docs/user-guide.pdf")
			}
		) {
			child(FormsToReview)
		}
	}
}

val FormsToReview = fc<Props> {
	val scope = useAsync()
	val allForms by useForms()

	val (client) = useClient()
	if (client !is Client.Authenticated) {
		p { text("Seuls les utilisateurs connectés peuvent voir la liste des formulaires qui les attendent") }
		return@fc
	}

	var forms by useState(emptyList<Form>()).asDelegated()
		.useListEquality()
	var loadingMessage by useState("Chargement des formulaires en cours…")
	if (forms.isEmpty())
		p { text(loadingMessage) }

	useEffect(client, allForms) {
		scope.reportExceptions {
			forms = client.todoList()
			loadingMessage = "Vous n'avez aucun formulaire à vérifier"
		}
	}

	for (form in forms) {
		child(FormDescription) {
			attrs {
				this.form = form
			}
		}
	}
}
