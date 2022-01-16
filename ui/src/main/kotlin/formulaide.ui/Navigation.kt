package formulaide.ui

import formulaide.api.data.Composite
import formulaide.api.data.Form
import formulaide.api.data.Record
import formulaide.api.data.RecordState
import formulaide.api.types.Email
import formulaide.api.types.Ref
import formulaide.api.users.User
import formulaide.ui.Role.Companion.role
import formulaide.ui.components.StyledButton
import formulaide.ui.components.TopBar
import formulaide.ui.components.cards.Card
import formulaide.ui.components.cards.action
import formulaide.ui.screens.*
import formulaide.ui.screens.data.CreateData
import formulaide.ui.screens.data.DataList
import formulaide.ui.screens.forms.edition.CreateForm
import formulaide.ui.screens.forms.list.FormList
import formulaide.ui.utils.GlobalState
import formulaide.ui.utils.useGlobalState
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.events.Event
import org.w3c.dom.url.URL
import react.*

private val currentScreen = GlobalState(getScreenFromWindow() ?: Screen.Home)
	.apply {
		subscribers.add("history push state" to {
			window.history.pushState(null,
			                         it.displayName,
			                         "?d=${it.route}")
		})
	}
	.apply {
		subscribers.add("document title" to {
			document.title = "${it.displayName} • Formulaide"
		})
	}

fun ChildrenBuilder.useNavigation() = useGlobalState(currentScreen)
fun navigateTo(screen: Screen) {
	currentScreen.value = screen
}

abstract class Screen(
	val displayName: String,
	val requiredRole: Role,
	val component: () -> FC<Props>,
	val route: String,
) {

	object Home : Screen("Accueil", Role.ANONYMOUS, { LoginAccess }, "home")
	object ShowData : Screen("Groupes", Role.ADMINISTRATOR, { DataList }, "data")
	object ShowForms : Screen("Formulaires", Role.ANONYMOUS, { FormList }, "forms")
	class NewData(original: Composite?) :
		Screen("Créer un groupe", Role.ADMINISTRATOR, { CreateData(original) }, "createData")

	class NewForm(original: Form?, copy: Boolean) :
		Screen("Créer un formulaire",
		       Role.ADMINISTRATOR,
		       { CreateForm(original, copy) },
		       "createForm")

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

	class Review(form: Form, state: RecordState?, records: List<Record>) :
		Screen("Vérification",
		       Role.EMPLOYEE,
		       { formulaide.ui.screens.Review(form, state, records) },
		       "review")

	companion object {
		private val regularScreens =
			sequenceOf(Home,
			           ShowData,
			           NewData(null),
			           ShowForms,
			           NewForm(null, copy = true),
			           ShowServices,
			           ShowUsers)

		fun availableScreens(user: User?) = regularScreens
			.filter { it.requiredRole <= user.role }

		fun routeDecoder(route: String): Screen? {
			val simpleRoutes = listOf(
				Home,
				ShowForms,
				NewData(null),
				NewForm(null, true),
				ShowUsers,
				NewUser,
				ShowServices
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

private val CannotAccessThisPage = FC<Props>("CannotAccessThisPage") {
	traceRenders("CannotAccessThisPage")

	Card {
		title = "Vous n'avez pas l'autorisation d'accéder à cette page"
		failed = true
		action("Retourner à la page d'accueil") { navigateTo(Screen.Home) }

		+"Si vous pensez que c'est anormal, veuillez contacter l'administrateur."
	}
}

val Navigation = FC<Props>("Navigation") {
	val user by useUser()
	var currentScreen by useNavigation()

	traceRenders("Navigation bar")

	for (screen in Screen.availableScreens(user)) {
		StyledButton {
			text = screen.displayName
			action = { currentScreen = screen }
			enabled = screen != currentScreen
		}
	}
}

private fun getScreenFromWindow(): Screen? =
	URL(window.location.href)
		.searchParams
		.get("d")
		?.let { Screen.routeDecoder(it) }

val Window = memo(FC("Window") {
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

	TopBar()

	if (user.role >= screen.requiredRole) {
		CrashReporter {
			screen.component()()
		}
	} else {
		CannotAccessThisPage()
	}
})
