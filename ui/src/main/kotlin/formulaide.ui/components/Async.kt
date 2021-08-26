package formulaide.ui.components

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import react.RBuilder
import react.useEffect
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

@Suppress("unused") // RBuilder is used for type safety
fun RBuilder.useAsyncEffect(vararg dependencies: dynamic, effect: suspend () -> Unit) {
	useEffect(dependencies) {
		val job = Job()

		CoroutineScope(job).launch {
			effect()
		}

		cleanup { job.cancel("Les dépendances de l'effet ont été modifiées") }
	}
}

@Suppress("unused") // RBuilder is used for type safety
fun RBuilder.useAsyncEffectOnce(effect: suspend () -> Unit) {
	useEffectOnce {
		val job = Job()

		CoroutineScope(job).launch {
			effect()
		}

		cleanup { job.cancel("Les dépendances de l'effet ont été modifiées") }
	}
}
