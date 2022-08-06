package formulaide.ui.navigation

import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.Snapshot
import formulaide.client.Client
import formulaide.ui.screens.Home
import formulaide.ui.theme.RailButton
import kotlinx.browser.window
import kotlinx.coroutines.launch

internal val productionUrl = window.location.protocol + "//" + window.location.host
internal const val localDevelopmentUrl = "http://localhost:8000"

private fun generateClient(production: Boolean): Client = when (production) {
	true -> Client.Anonymous.connect(productionUrl)
	false -> Client.Anonymous.connect(localDevelopmentUrl)
}

var client: Client by mutableStateOf(generateClient(production = true))

@Composable
fun SelectProductionOrTest() {
	LaunchedEffect(client) {
		try {
			client.forms.all()
		} catch (e: Exception) {
			client = generateClient(production = false)
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
				(client as? Client.Authenticated)?.logout()

				Snapshot.withMutableSnapshot {
					client = generateClient(production = true)
					currentScreen = Home
				}
			}
		}
	)
}
