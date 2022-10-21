package formulaide.ui.components

import androidx.compose.runtime.*
import formulaide.ui.theme.Theme
import formulaide.ui.theme.shade
import io.ktor.client.plugins.*
import io.ktor.client.statement.*
import opensavvy.state.Slice
import opensavvy.state.Status
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

@Composable
fun DisplayError(data: Slice<Any>) {
	val status = data.status

	if (status is Status.Failed) {
		val statusMessage = status.message ?: "Erreur inconnue"
		val cause = status.cause

		val message = when (status) {
			is Status.StandardFailure -> when (status.kind) {
				Status.StandardFailure.Kind.Invalid -> "Invalide : $statusMessage"
				Status.StandardFailure.Kind.Unauthenticated -> "Réservé aux utilisateurs connectés : $statusMessage"
				Status.StandardFailure.Kind.Unauthorized -> "Droits manquants : $statusMessage"
				Status.StandardFailure.Kind.NotFound -> "Introuvable : $statusMessage"
				Status.StandardFailure.Kind.Unknown -> statusMessage
			}

			else -> statusMessage
		}

		DisplayError(message)

		if (cause != null)
			DisplayError(cause)
	}
}
