package formulaide.ui

import formulaide.ui.components.styledCard
import formulaide.ui.utils.text
import io.ktor.client.call.*
import io.ktor.client.features.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import react.fc
import react.useEffect
import react.useState

external interface ErrorProps : ScreenProps {
	var error: Throwable

	var hide: () -> Unit
}

val ErrorCard = fc<ErrorProps> { props ->
	val error = props.error

	var title by useState(error.message ?: error.toString())
	var subtitle by useState(error::class.simpleName ?: error::class.toString())
	var body by useState<String>()

	useEffect(error) {
		props.scope.launch {
			when (error) {
				is ResponseException -> {
					val responseText = error.response.receive<String>()

					if (responseText.isBlank()) {
						title = error.response.status.toString()
					} else {
						title = responseText
						subtitle = "${error.response.status} • $subtitle"
					}

					body = "Le serveur a refusé la connexion."
				}
			}
		}
	}

	styledCard(
		title,
		subtitle,
		"OK" to { props.hide() },
		failed = true,
	) { body?.let { text(it) } }
}

inline fun <R> reportExceptions(reporter: (Throwable) -> Unit, block: () -> R): R = try {
	block()
} catch (e: Exception) {
	if (e !is CancellationException) {
		// don't report jobs being cancelled, that's normal

		console.error(e)
		reporter(e)
	}
	throw RuntimeException("Impossible de continuer", cause = e)
}

inline fun <R> reportExceptions(props: ScreenProps, block: () -> R) =
	reportExceptions(props.reportError, block)

inline fun launchAndReportExceptions(
	crossinline reporter: (Throwable) -> Unit,
	scope: CoroutineScope,
	crossinline block: suspend () -> Unit,
) =
	scope.launch {
		reportExceptions(reporter) {
			block()
		}
	}

inline fun launchAndReportExceptions(props: ScreenProps, crossinline block: suspend () -> Unit) =
	launchAndReportExceptions(props.reportError, props.scope, block)
