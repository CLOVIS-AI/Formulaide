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
import formulaide.ui.components.cards.Card
import formulaide.ui.components.cards.FormCard
import formulaide.ui.components.cards.action
import formulaide.ui.components.cards.submit
import formulaide.ui.components.inputs.Field
import formulaide.ui.components.inputs.Input
import formulaide.ui.components.useAsync
import formulaide.ui.screens.forms.list.FormDescription
import formulaide.ui.utils.DelegatedProperty.Companion.asDelegated
import formulaide.ui.utils.useListEquality
import kotlinx.browser.window
import org.w3c.dom.HTMLInputElement
import react.*
import react.dom.html.InputType
import react.dom.html.ReactHTML.p

/**
 * A login widget that requests an email and a password, and then updates the application's [client][client] by connecting to the server.
 *
 * @see Client
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

@Suppress("FunctionName")
fun PasswordModification(user: Email, previousScreen: Screen) = FC<Props>("PasswordModification") {
	traceRenders("PasswordModification")

	val oldPassword = useRef<HTMLInputElement>()
	val newPassword1 = useRef<HTMLInputElement>()
	val newPassword2 = useRef<HTMLInputElement>()

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

		Field {
			id = "old-password"
			text = "Mot de passe actuel"

			Input {
				type = InputType.password
				id = "old-password"
				ref = oldPassword
				required = !me.administrator
			}
		}

		Field {
			id = "new-password-1"
			text = "Nouveau mot de passe"

			Input {
				type = InputType.password
				id = "new-password-1"
				required = true
				ref = newPassword1
			}
		}

		Field {
			id = "new-password-2"
			text = "Confirmer le nouveau mot de passe"

			Input {
				type = InputType.password
				id = "new-password-2"
				required = true
				ref = newPassword2
			}
		}
	}
}

val LoginAccess = FC<Props>("LoginAccess") {
	traceRenders("LoginAccess")

	val (user) = useUser("LoginAccess")

	if (user == null) {
		Login()
	} else {
		Card {
			title = "Espace employé"

			action("Déconnexion") { logout() }
			action("Modifier mon mot de passe") { navigateTo(Screen.EditPassword(user.email, Screen.Home)) }
			action("Aide") { window.open("https://clovis-ai.gitlab.io/formulaide/docs/user-guide.pdf") }

			FormsToReview()
		}
	}
}

val FormsToReview = FC<Props>("FormsToReview") {
	val scope = useAsync()
	val allForms by useForms()

	val (client) = useClient()
	if (client !is Client.Authenticated) {
		p { +"Seuls les utilisateurs connectés peuvent voir la liste des formulaires qui les attendent" }
		return@FC
	}

	var forms by useState(emptyList<Form>())
		.asDelegated()
		.useListEquality()
	var loadingMessage by useState("Chargement des formulaires en cours…")
	if (forms.isEmpty())
		p { +loadingMessage }

	useEffect(client, allForms) {
		scope.reportExceptions {
			forms = client.todoList()
			loadingMessage = "Vous n'avez aucun formulaire à vérifier"
		}
	}

	for (form in forms.sortedBy { it.name }) {
		FormDescription {
			this.form = form
		}
	}
}
