package formulaide.ui

import formulaide.api.data.Composite
import formulaide.api.data.Form
import formulaide.api.users.User
import formulaide.client.Client
import formulaide.ui.Role.Companion.role
import formulaide.ui.auth.LoginAccess
import formulaide.ui.components.styledButton
import formulaide.ui.components.styledCard
import formulaide.ui.screens.CreateData
import formulaide.ui.screens.CreateForm
import formulaide.ui.screens.FormList
import formulaide.ui.utils.text
import kotlinx.coroutines.CoroutineScope
import kotlinx.html.js.onClickFunction
import react.*
import react.dom.*

abstract class Screen(
	val displayName: String,
	val requiredRole: Role,
	val component: FunctionalComponent<ScreenProps>
) {

	object Home : Screen("Page d'acceuil", Role.ANONYMOUS, LoginAccess)
	object ShowForms : Screen("Liste des formulaires", Role.ANONYMOUS, FormList)
	object NewData : Screen("Créer une donnée", Role.ADMINISTRATOR, CreateData)
	object NewForm : Screen("Créer un formulaire", Role.ADMINISTRATOR, CreateForm)

	class SubmitForm(form: Form) : Screen("Saisie", Role.ANONYMOUS, formulaide.ui.screens.SubmitForm(form))

	companion object {
		val regularScreens = sequenceOf(Home, ShowForms, NewData, NewForm)
		fun availableScreens(user: User?) = regularScreens
			.filter { it.requiredRole <= user.role }
	}
}

external interface ApplicationProps : RProps {

	// User connection
	var client: Client
	var user: User?
	var connect: (Client) -> Unit

	// Async
	var scope: CoroutineScope

	// Common data
	var composites: List<Composite>
	var forms: List<Form>
	var refreshComposites: () -> Unit
	var refreshForms: () -> Unit
}

external interface ScreenProps : ApplicationProps {

	// Navigation
	var currentScreen: Screen
	var navigateTo: (Screen) -> Unit
}

private val CannotAccessThisPage = functionalComponent<ScreenProps> { props ->
	p { text("Vous n'avez pas l'autorisation d'accéder à cette page.") }

	br {}
	button {
		text("Retourner à la page d'accueil")
		attrs { onClickFunction = { props.navigateTo(Screen.Home) } }
	}
}

private val Navigation = functionalComponent<ScreenProps> { props ->
	div {
		for (screen in Screen.availableScreens(props.user)) {
			if (screen != props.currentScreen)
				styledButton(screen.displayName) { props.navigateTo(screen) }
			else
				styledDisabledButton(screen.displayName)
		}
	}
}

val Window = functionalComponent<ApplicationProps> { props ->
	val (screen, setScreen) = useState<Screen>(Screen.Home)

	val title = "Formulaide • ${screen.displayName}"
	val subtitle = when (props.user) {
		null -> "Accès anonyme"
		else -> "Bonjour ${props.user!!.fullName}"
	}

	styledCard(title, subtitle) {
		child(Navigation) {
			attrs {
				navigateTo = { setScreen(it) }
				currentScreen = screen
				user = props.user
				connect = props.connect
			}
		}
	}

	if (screen.requiredRole > props.user.role) {
		child(CannotAccessThisPage) {
			attrs { navigateTo = { setScreen(it) } }
		}
	} else child(screen.component) {
		attrs {
			client = props.client
			user = props.user
			connect = props.connect

			scope = props.scope
			composites = props.composites
			forms = props.forms
			refreshComposites = props.refreshComposites
			refreshForms = props.refreshForms

			currentScreen = screen
			navigateTo = { setScreen(it) }
		}
	}
}
