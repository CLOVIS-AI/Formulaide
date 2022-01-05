package formulaide.ui

import formulaide.ui.components.styledCard
import formulaide.ui.components.useAsync
import formulaide.ui.utils.GlobalState
import formulaide.ui.utils.text
import formulaide.ui.utils.useGlobalState
import io.ktor.client.call.*
import io.ktor.client.features.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import react.*

private val errors = GlobalState(listOf<Throwable>())
fun ChildrenBuilder.useErrors(): List<Throwable> {
	val (local, _) = useGlobalState(errors)

	return local
}

fun reportError(error: Throwable) {
	errors.value = errors.value + error
}

external interface ErrorProps : Props {
	var error: Throwable
}

val ErrorCard = FC<ErrorProps>("ErrorCard") { props ->
	traceRenders("ErrorCard")

	var errors by useGlobalState(errors)
	val error = props.error

	val scope = useAsync()

	var title by useState(error.message ?: error.toString())
	var subtitle by useState(error::class.simpleName ?: error::class.toString())
	var body by useState<String>()

	useEffect(error) {
		scope.launch {
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
		"OK" to {
			val index = errors.indexOf(error)
			errors = errors.subList(0, index) + errors.subList(index + 1, errors.size)
		},
		failed = true,
	) { body?.let { text(it) } }
}

inline fun <R> reportExceptions(finally: (e: Exception) -> Unit = {}, block: () -> R): R = try {
	block()
} catch (e: CancellationException) {
	console.info("Job was cancelled", e)
	throw e
} catch (e: Exception) {
	console.error(e)
	reportError(e)
	finally(e)
	throw RuntimeException("Impossible de continuer", cause = e)
}

inline fun CoroutineScope.reportExceptions(
	crossinline onFailure: (e: Exception) -> Unit = {},
	crossinline block: suspend () -> Unit,
) =
	launch {
		formulaide.ui.reportExceptions(onFailure) {
			block()
		}
	}
