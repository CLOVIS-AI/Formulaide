package formulaide.ui.components

import formulaide.ui.reportExceptions
import kotlinx.coroutines.*
import react.ChildrenBuilder
import react.useEffect
import react.useEffectOnce
import react.useState

/**
 * Returns a [CoroutineScope] that is active as long as this component is mounted.
 *
 * Use this hook to launch code that should be cancelled if the component that started it is unmounted.
 */
@Suppress("unused") // ChildrenBuilder is used for type safety
fun ChildrenBuilder.useAsync(): CoroutineScope {
	val job by useState(SupervisorJob())

	useEffectOnce {
		cleanup {
			job.cancel("Le composant a été dé-monté")
		}
	}

	return CoroutineScope(job)
}

@Suppress("unused") // ChildrenBuilder is used for type safety
fun ChildrenBuilder.useAsyncEffect(vararg dependencies: dynamic, effect: suspend () -> Unit) {
	useEffect(*dependencies) {
		val job = Job()

		CoroutineScope(job).launch {
			reportExceptions {
				effect()
			}
		}

		cleanup { job.cancel("Les dépendances de l'effet ont été modifiées") }
	}
}

@Suppress("unused") // ChildrenBuilder is used for type safety
fun ChildrenBuilder.useAsyncEffectOnce(effect: suspend () -> Unit) {
	useEffectOnce {
		val job = Job()

		CoroutineScope(job).launch {
			reportExceptions {
				effect()
			}
		}

		cleanup { job.cancel("Les dépendances de l'effet ont été modifiées") }
	}
}
