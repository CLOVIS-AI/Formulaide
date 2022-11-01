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
import opensavvy.state.firstValueOrNull
import opensavvy.state.slice.valueOrNull

internal val productionUrl = window.location.protocol + "//" + window.location.host
internal const val localDevelopmentUrl = "https://api.localhost:8443"

var client: Client by mutableStateOf(Client(productionUrl))

@Composable
fun SelectProductionOrTest() {
	LaunchedEffect(Unit) {
		var candidate = client

		if (candidate.ping().isLeft()) {
			console.warn("Could not ping the production server, switching to the development server…")
			candidate = Client(localDevelopmentUrl)
		}

		val myRef = candidate.users.me().orNull()
		if (myRef != null) {
			val me = myRef.request().firstValueOrNull()
			if (me != null) {
				console.info("We're currently logged in")
				candidate.context.value = Context(myRef, me.role)
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
					client.users.logOut().valueOrNull

				Snapshot.withMutableSnapshot {
					client = Client(productionUrl)
					currentScreen = Home
				}
			}
		}
	)
}
