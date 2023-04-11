package opensavvy.formulaide.test.structure

import arrow.core.continuations.EffectScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.TestCoroutineScheduler

interface TestScope : EffectScope<Any> {

    val testJob: Job
        get() = testScope.coroutineContext[Job]
            ?: error("Couldn't find a job in the testScope")

    val testScope: CoroutineScope

    val backgroundJob: Job
        get() = backgroundScope.coroutineContext[Job]
            ?: error("Couldn't find a job in the backgroundScope")

    val backgroundScope: CoroutineScope

    val scheduler: TestCoroutineScheduler

}
