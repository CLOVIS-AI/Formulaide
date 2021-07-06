package formulaide.ui

import formulaide.ui.components.styledCard
import formulaide.ui.utils.text
import kotlinx.coroutines.launch
import react.functionalComponent

external interface ErrorProps : ScreenProps {
	var error: Throwable
}

val ErrorCard = functionalComponent<ErrorProps> { props ->
	val error = props.error

	styledCard(
		error.message ?: error.toString(),
		error::class.simpleName,
		failed = true
	) {
		text("Here, in the future: an error message")
	}
}

inline fun reportExceptions(reporter: (Throwable) -> Unit, block: () -> Unit) {
	try {
		block()
	} catch (e: Exception) {
		console.error(e)
		reporter(e)
	}
}

inline fun reportExceptions(props: ScreenProps, block: () -> Unit) =
	reportExceptions(props.reportError, block)

inline fun launchAndReportExceptions(props: ScreenProps, crossinline block: suspend () -> Unit) =
	props.scope.launch {
		reportExceptions(props) {
			block()
		}
	}
