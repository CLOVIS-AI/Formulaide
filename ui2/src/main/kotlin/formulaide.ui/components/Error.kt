package formulaide.ui.components

import androidx.compose.runtime.*
import arrow.core.Either
import formulaide.ui.theme.Theme
import formulaide.ui.theme.shade
import io.ktor.client.plugins.*
import io.ktor.client.statement.*
import opensavvy.state.Failure
import opensavvy.state.slice.Slice
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
fun DisplayError(failure: Failure?) {
	if (failure != null) {
		val statusMessage = failure.message
		val cause = failure.cause

		val message = when (failure.kind) {
			Failure.Kind.Invalid -> "Invalide : $statusMessage"
			Failure.Kind.Unauthenticated -> "Réservé aux utilisateurs connectés : $statusMessage"
			Failure.Kind.Unauthorized -> "Droits manquants : $statusMessage"
			Failure.Kind.NotFound -> "Introuvable : $statusMessage"
			Failure.Kind.Unknown -> statusMessage
		}

		DisplayError(message)

		if (cause != null)
			DisplayError(cause)
	}
}

@Composable
fun DisplayError(data: Slice<Any>?) {
	if (data is Either.Left) {
		DisplayError(data.value)
	}
}

@Composable
fun DisplayError(failure: State<Failure?>) {
	val current by failure
	DisplayError(current)
}
