package formulaide.ui.navigation

import androidx.compose.runtime.*
import formulaide.ui.screens.DummyScreen
import formulaide.ui.screens.Home
import kotlinx.browser.document
import kotlinx.browser.window
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*
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

val screens = listOf(
	Home,
	DummyScreen,
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
					marginTop(8.px)
					marginBottom(8.px)
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
			justifyContent(JustifyContent.Center)
			alignItems(AlignItems.Center)
			gap(30.px)
		}
	}) {
	for (screen in screens) {
		NavigationTarget(screen)
	}
}

@Composable
private fun NavigationTarget(screen: Screen) {
	val selected by remember { derivedStateOf { screen == currentScreen } }

	@Composable
	fun NavigationTargetInner() {
		I(
			{
				classes(if (selected) screen.iconSelected else screen.icon)

				style {
					property("font-size", "xx-large")
				}
			})

		if (selected) {
			Br()
			Text(screen.title)
		}
	}

	Button(
		{
			onClick { currentScreen = screen }

			title(screen.title)
		}) {
		NavigationTargetInner()
	}
}

fun loadNavigation() {
	val url = URL(window.location.href)
		.search

	val params = url.split("&")
	console.log("Loading the navigation system... Requested route: $params")

	val target = params.getOrNull(0) ?: Home.route
	val screen = screens
		.firstOrNull { it.route.startsWith(target) }
	console.log("...it refers to the screen $screen")

	if (screen != null)
		currentScreen = screen
}
