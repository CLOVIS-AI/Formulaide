package formulaide.ui.navigation

import androidx.compose.runtime.*
import formulaide.ui.screens.*
import formulaide.ui.theme.RailButton
import formulaide.ui.theme.ThemeSelector
import formulaide.ui.utils.role
import kotlinx.browser.document
import kotlinx.browser.window
import opensavvy.formulaide.core.User
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Nav
import org.w3c.dom.events.Event
import org.w3c.dom.url.URL

@Suppress("ObjectPropertyName")
private var _currentScreen by mutableStateOf(Home)

var currentScreen: Screen
	get() = _currentScreen
	set(value) {
		window.history.pushState(null, value.title, value.route)
		document.title = "${value.title} • Formulaide"
		_currentScreen = value
	}

val rootScreens = listOf(
	Home,
	TemplateList,
	FormList,
	DepartmentList,
	UserList,
)

val allScreens = rootScreens + listOf(
	FormEditor,
	TemplateCreator,
	DepartmentCreator,
	UserCreator,
	PasswordModification,
)

@Composable
fun Navigation() {
	DisposableEffect(Unit) {
		val handler = { _: Event ->
			loadNavigation()
		}

		window.addEventListener("popstate", handler)
		onDispose { window.removeEventListener("popstate", handler) }
	}

	Div({
		    id("Navigation")

		    style {
			    display(DisplayStyle.Grid)
			    gridTemplateColumns("80px auto")
			    gap(8.px)
			    marginLeft(8.px)
			    marginRight(8.px)
		    }
	    }) {
		NavigationRail()
		Div(
			{
				id("Body")

				style {
					marginTop(20.px)
					marginBottom(15.px)
				}
			}) {
			_currentScreen()
		}
	}
}

@Composable
private fun NavigationRail() = Nav(
	{
		id("NavigationRail")

		style {
			height(100.vh)
			position(Position.Sticky)
			top(0.px)
			display(DisplayStyle.Flex)
			flexDirection(FlexDirection.Column)
			justifyContent(JustifyContent.SpaceBetween)
			paddingTop(30.px)
			paddingBottom(30.px)
		}
	}) {

	NavigationArea("actions") {
		currentScreen.parent?.let { parentScreen ->
			RailButton(
				"ri-arrow-left-line",
				"ri-arrow-left-fill",
				"Retourner à la page ${parentScreen.title}",
				selected = false,
				action = { currentScreen = parentScreen }
			)
		}

		currentScreen.actions()
	}

	NavigationArea("screens") {
		val role = client.role
		val visibleScreens by derivedStateOf { rootScreens.filter { role >= it.requiredRole } }

		for (screen in visibleScreens) {
			RailButton(
				screen.icon,
				screen.iconSelected,
				screen.title,
				selected = screen == currentScreen,
				action = { currentScreen = screen }
			)
		}
	}

	NavigationArea("settings") {
		if (client.role >= User.Role.EMPLOYEE)
			LogOutButton()

		ThemeSelector()
	}
}

@Composable
private fun NavigationArea(id: String, block: @Composable () -> Unit) = Div(
	{
		id(id)

		style {
			display(DisplayStyle.Flex)
			flexDirection(FlexDirection.Column)
			justifyContent(JustifyContent.Center)
			alignItems(AlignItems.Center)
			gap(30.px)
		}
	}
) {
	block()
}

fun loadNavigation() {
	val url = URL(window.location.href)
		.search

	val params = url.split("&")
	console.log("Loading the navigation system... Requested route: $params")

	val target = params.getOrNull(0) ?: Home.route
	val screen = allScreens
		.firstOrNull { it.route.startsWith(target) }
	console.log("...it refers to the screen $screen")

	if (screen != null)
		currentScreen = screen
}
