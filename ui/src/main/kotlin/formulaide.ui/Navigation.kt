package formulaide.ui

import formulaide.api.data.Form
import formulaide.api.data.Record
import formulaide.api.data.RecordState
import formulaide.api.types.Email
import formulaide.api.types.Ref
import formulaide.api.users.User
import formulaide.ui.Role.Companion.role
import formulaide.ui.components.styledButton
import formulaide.ui.components.styledCard
import formulaide.ui.components.styledDisabledButton
import formulaide.ui.screens.*
import formulaide.ui.utils.GlobalState
import formulaide.ui.utils.text
import formulaide.ui.utils.useGlobalState
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.events.Event
import org.w3c.dom.url.URL
import react.*
import react.dom.div

private val currentScreen = GlobalState(getScreenFromWindow() ?: Screen.Home)
	.apply { subscribers.add { window.history.pushState(null, it.displayName, "?d=${it.route}") } }
	.apply { subscribers.add { document.title = "${it.displayName} • Formulaide" } }

fun RBuilder.useNavigation() = useGlobalState(currentScreen)
fun navigateTo(screen: Screen) {
	currentScreen.value = screen
}

abstract class Screen(
	val displayName: String,
	val requiredRole: Role,
	val component: () -> FC<RProps>,
	val route: String,
) {

	object Home : Screen("Accueil", Role.ANONYMOUS, { LoginAccess }, "home")
	object ShowData : Screen("Groupes", Role.EMPLOYEE, { DataList }, "data")
	object ShowForms : Screen("Formulaires", Role.ANONYMOUS, { FormList }, "forms")
	object NewData : Screen("Créer un groupe", Role.ADMINISTRATOR, { CreateData }, "createData")
	object NewForm : Screen("Créer un formulaire", Role.ADMINISTRATOR, { CreateForm }, "createForm")
	object ShowUsers : Screen("Employés", Role.ADMINISTRATOR, { UserList }, "employees")
	object NewUser :
		Screen("Créer un employé", Role.ADMINISTRATOR, { CreateUser }, "createEmployee")

	object ShowServices : Screen("Services", Role.ADMINISTRATOR, { ServiceList }, "services")

	class EditPassword(user: Email, redirectTo: Screen) :
		Screen("Modifier mon mot de passe",
		       Role.EMPLOYEE,
		       { PasswordModification(user, redirectTo) },
		       "editUser-${user.email}")

	class SubmitForm(form: Ref<Form>) :
		Screen("Saisie",
		       Role.ANONYMOUS,
		       { formulaide.ui.screens.SubmitForm(form) },
		       "submit-${form.id}")

	class Review(form: Form, state: RecordState, records: List<Record>) :
		Screen("Vérification",
		       Role.EMPLOYEE,
		       { formulaide.ui.screens.Review(form, state, records) },
		       "review")

	companion object {
		val regularScreens =
			sequenceOf(Home, ShowData, NewData, ShowForms, NewForm, ShowServices, ShowUsers)

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

private val CannotAccessThisPage = fc<RProps> {
	traceRenders("CannotAccessThisPage")

	styledCard(
		"Vous n'avez pas l'autorisation d'accéder à cette page",
		null,
		"Retourner à la page d'accueil" to { navigateTo(Screen.Home) },
		failed = true
	) {
		text("Si vous pensez que c'est anormal, veuillez contacter l'administrateur.")
	}
}

private val Navigation = fc<RProps> {
	val user by useUser()
	var currentScreen by useNavigation()

	traceRenders("Navigation bar")

	div {
		for (screen in Screen.availableScreens(user)) {
			if (screen != currentScreen)
				styledButton(screen.displayName) { currentScreen = screen }
			else
				styledDisabledButton(screen.displayName)
		}
	}
}

private fun getScreenFromWindow(): Screen? =
	URL(window.location.href)
		.searchParams
		.get("d")
		?.let { Screen.routeDecoder(it) }

val Window = fc<RProps> {
	var screen by useNavigation()
	val user by useUser()

	traceRenders("Window, screen ${screen.displayName}")

	useEffectOnce {
		val handler = { _: Event ->
			screen = getScreenFromWindow() ?: Screen.Home
		}
		window.addEventListener("popstate", handler)

		cleanup {
			window.removeEventListener("popstate", handler)
		}
	}

	val title = "Formulaide • ${screen.displayName}"
	val subtitle = when (user) {
		null -> "Accès anonyme"
		else -> "Bonjour ${user!!.fullName}"
	}

	styledCard(title, subtitle) {
		child(Navigation)
	}

	if (user.role >= screen.requiredRole) {
		child(screen.component())
	} else {
		child(CannotAccessThisPage)
	}
}
