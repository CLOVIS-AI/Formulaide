package formulaide.ui.navigation

import androidx.compose.runtime.*
import formulaide.client.Client
import kotlinx.browser.window

internal val productionUrl = window.location.protocol + "//" + window.location.host
internal const val localDevelopmentUrl = "http://localhost:8000"

private fun generateClient(production: Boolean): Client = when (production) {
	true -> Client.Anonymous.connect(productionUrl)
	false -> Client.Anonymous.connect(localDevelopmentUrl)
}

var client: Client by mutableStateOf(generateClient(production = true))

@Composable
fun SelectProductionOrTest() {
	LaunchedEffect(Unit) {
		try {
			client.forms.all()
		} catch (e: Exception) {
			client = generateClient(production = false)
		}
	}
}
