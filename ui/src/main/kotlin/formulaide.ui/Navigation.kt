package formulaide.ui

import formulaide.api.data.Composite
import formulaide.api.data.Form
import formulaide.api.types.Email
import formulaide.api.types.Ref
import formulaide.api.users.Service
import formulaide.api.users.User
import formulaide.client.Client
import formulaide.ui.Role.Companion.role
import formulaide.ui.auth.LoginAccess
import formulaide.ui.auth.PasswordModification
import formulaide.ui.components.styledButton
import formulaide.ui.components.styledCard
import formulaide.ui.components.styledDisabledButton
import formulaide.ui.screens.*
import formulaide.ui.utils.text
import kotlinx.coroutines.CoroutineScope
import org.w3c.dom.url.URL
import react.*
import react.dom.div

abstract class Screen(
	val displayName: String,
	val requiredRole: Role,
	val component: FunctionalComponent<ScreenProps>,
	val route: String,
) {

	object Home : Screen("Acceuil", Role.ANONYMOUS, LoginAccess, "home")
	object ShowForms : Screen("Formulaires", Role.ANONYMOUS, FormList, "forms")
	object NewData : Screen("Créer une donnée", Role.ADMINISTRATOR, CreateData, "createData")
	object NewForm : Screen("Créer un formulaire", Role.ADMINISTRATOR, CreateForm, "createForm")
	object ShowUsers : Screen("Employés", Role.ADMINISTRATOR, UserList, "employees")
	object NewUser : Screen("Créer un employé", Role.ADMINISTRATOR, CreateUser, "createEmployee")
	object ShowServices : Screen("Services", Role.ADMINISTRATOR, ServiceList, "services")

	class EditPassword(user: Email, redirectTo: Screen) :
		Screen("Modifier mon mot de passe",
		       Role.EMPLOYEE,
		       PasswordModification(user, redirectTo),
		       "editUser-${user.email}")

	class SubmitForm(form: Ref<Form>) :
		Screen("Saisie",
		       Role.ANONYMOUS,
		       formulaide.ui.screens.SubmitForm(form),
		       "submit-${form.id}")

	companion object {
		val regularScreens = sequenceOf(Home, ShowForms, NewData, NewForm, ShowServices, ShowUsers)
		fun availableScreens(user: User?) = regularScreens
			.filter { it.requiredRole <= user.role }

		fun routeDecoder(route: String): Screen? {
			val simpleRoutes = listOf(
				Home, ShowForms, NewData, NewForm, ShowUsers, NewUser, ShowServices
			)
			for (screen in simpleRoutes)
				if (route == screen.route)
					return screen

			return when {
				route.startsWith("editUser-") -> EditPassword(Email(route.split('-')[1]), Home)
				route.startsWith("submit-") -> SubmitForm(Ref(route.split('-')[1]))
				else -> null
			}
		}
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
	var services: List<Service>
	var refreshComposites: () -> Unit
	var refreshForms: () -> Unit
	var refreshServices: () -> Unit

	// Failures
	var reportError: (Throwable) -> Unit
}

external interface ScreenProps : ApplicationProps {

	// Navigation
	var currentScreen: Screen
	var navigateTo: (Screen) -> Unit

}

private val CannotAccessThisPage = functionalComponent<ScreenProps> { props ->
	styledCard(
		"Vous n'avez pas l'autorisation d'accéder à cette page",
		null,
		"Retourner à la page d'accueil" to { props.navigateTo(Screen.Home) },
		failed = true
	) {
		text("Si vous pensez que c'est anormal, veuillez contacter l'administrateur.")
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

	val setProps = { it: ScreenProps ->
		with(it) {
			client = props.client
			user = props.user
			connect = props.connect

			scope = props.scope
			composites = props.composites
			forms = props.forms
			services = props.services
			refreshComposites = props.refreshComposites
			refreshForms = props.refreshForms
			refreshServices = props.refreshServices

			currentScreen = screen
			navigateTo = { setScreen(it) }

			reportError = props.reportError
		}
	}

	styledCard(title, subtitle) {
		child(Navigation) {
			attrs {
				setProps(this)
			}
		}
	}

	if (props.user.role >= screen.requiredRole) {
		child(screen.component) {
			attrs { setProps(this) }
		}
	} else {
		child(CannotAccessThisPage) {
			attrs { setProps(this) }
		}
	}
}
