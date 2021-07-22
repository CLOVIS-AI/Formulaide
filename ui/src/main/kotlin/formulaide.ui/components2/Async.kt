package formulaide.ui.components2

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import react.RBuilder
import react.useEffectOnce
import react.useState

@Suppress("unused") // RBuilder is used for type safety
fun RBuilder.useAsync(): CoroutineScope {
	val job by useState(Job())

	useEffectOnce {
		cleanup {
			job.cancel("Le composant a été dé-monté")
		}
	}

	return CoroutineScope(job)
}
