package formulaide.ui.navigation

import androidx.compose.runtime.*
import formulaide.ui.screens.Home
import formulaide.ui.theme.RailButton
import kotlinx.browser.window
import kotlinx.coroutines.launch
import opensavvy.backbone.Ref.Companion.request
import opensavvy.formulaide.api.Context
import opensavvy.formulaide.api.client.Client
import opensavvy.formulaide.core.User
import opensavvy.state.firstValueOrNull
import opensavvy.state.slice.valueOrNull
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.height
import org.jetbrains.compose.web.css.vh
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text

internal val productionUrl = window.location.protocol + "//" + window.location.host
internal const val localDevelopmentUrl = "https://api.localhost:8443"

var client: Client by mutableStateOf(Client(productionUrl))

@Composable
fun SelectProductionOrTest(content: @Composable () -> Unit) {
	var message by remember { mutableStateOf<String?>("Chargement…") }

	LaunchedEffect(Unit) {
		var candidate = client

		message = "Connexion…"
		try {
			if (candidate.ping().isLeft()) {
				console.warn("Could not ping the production server, switching to the development server…")
				candidate = Client(localDevelopmentUrl)
			}

			message = "Vérification des identifiants…"
			val myRef = candidate.users.me().orNull()
			if (myRef != null) {
				val me = myRef.request().firstValueOrNull()
				if (me != null) {
					console.info("We're currently logged in")
					candidate.context.value = Context(myRef, me.role)
				}
			}
		} catch (e: Exception) {
			console.error("Error while attempting to connect to the server", e)
		}

		client = candidate
		message = null
	}

	if (message != null) {
		Div(
			{
				style {
					display(DisplayStyle.Grid)
					property("place-items", "center")
					height(100.vh)
				}
			}
		) {
			Text(message!!)
		}
	} else content()
}

@Composable
fun LogOutButton() {
	val scope = rememberCoroutineScope()

	RailButton(
		"ri-logout-box-r-line",
		"ri-logout-box-r-fill",
		"Déconnexion",
		selected = false,
		action = {
			scope.launch {
				if (client.context.value.role >= User.Role.EMPLOYEE)
					client.users.logOut().valueOrNull

				currentScreen = Home
			}
		}
	)
}
