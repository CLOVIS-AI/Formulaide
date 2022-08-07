package formulaide.ui.components

import androidx.compose.runtime.*
import formulaide.ui.theme.Theme
import formulaide.ui.theme.shade
import io.ktor.client.plugins.*
import io.ktor.client.statement.*
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text

@Composable
fun DisplayError(error: Throwable) {
	var message by remember { mutableStateOf(error.toString()) }

	LaunchedEffect(error) {
		when {
			error is ClientRequestException -> message = error.response.bodyAsText()
			error.message != null -> message = error.message!!
		}
	}

	DisplayError(message)
}

@Composable
fun DisplayError(error: String) = P(
	{
		style {
			shade(Theme.current.error)
			property("font-size", "small")
		}
	}
) {
	Text(error)
}
