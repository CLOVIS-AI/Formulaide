package formulaide.ui.navigation

import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.Snapshot
import formulaide.ui.screens.Home
import formulaide.ui.theme.RailButton
import kotlinx.browser.window
import kotlinx.coroutines.launch
import opensavvy.backbone.Ref.Companion.request
import opensavvy.formulaide.api.Context
import opensavvy.formulaide.api.client.Client
import opensavvy.formulaide.core.User
import opensavvy.state.Status
import opensavvy.state.firstResult
import opensavvy.state.firstResultOrThrow

internal val productionUrl = window.location.protocol + "//" + window.location.host
internal const val localDevelopmentUrl = "https://api.localhost:8443"

var client: Client by mutableStateOf(Client(productionUrl))

@Composable
fun SelectProductionOrTest() {
	LaunchedEffect(Unit) {
		var candidate = client

		if (candidate.ping().firstResult().status !is Status.Successful) {
			console.warn("Could not ping the production server, switching to the development server…")
			candidate = Client(localDevelopmentUrl)
		}

		val myRef = candidate.users.me().firstResult().status
		if (myRef is Status.Successful) {
			val me = myRef.value.request().firstResult().status
			if (me is Status.Successful) {
				console.info("We're currently logged in")
				candidate.context.value = Context(myRef.value, me.value.role)
			}
		}

		client = candidate
	}
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
					client.users.logOut().firstResultOrThrow()

				Snapshot.withMutableSnapshot {
					client = Client(productionUrl)
					currentScreen = Home
				}
			}
		}
	)
}
