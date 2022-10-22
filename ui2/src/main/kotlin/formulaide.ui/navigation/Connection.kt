package formulaide.ui.navigation

import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.Snapshot
import formulaide.ui.screens.Home
import formulaide.ui.theme.RailButton
import kotlinx.browser.window
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import opensavvy.formulaide.api.client.Client
import opensavvy.formulaide.core.User
import opensavvy.state.firstResultOrThrow

internal val productionUrl = window.location.protocol + "//" + window.location.host
internal const val localDevelopmentUrl = "https://api.localhost:8443"

var client: Client by mutableStateOf(Client(productionUrl))

@Composable
fun SelectProductionOrTest() {
	LaunchedEffect(client) {
		try {
			client.ping()
				.onEach { console.log("Pinging the server…", it) }
				.firstResultOrThrow()
		} catch (e: Exception) {
			client = Client(localDevelopmentUrl)
		}
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
				if (client.role >= User.Role.EMPLOYEE)
					client.users.logOut().firstResultOrThrow()

				Snapshot.withMutableSnapshot {
					client = Client(productionUrl)
					currentScreen = Home
				}
			}
		}
	)
}
