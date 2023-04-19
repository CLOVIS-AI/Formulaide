package opensavvy.formulaide.test.structure

import arrow.core.raise.Raise
import arrow.core.raise.RaiseDSL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.TestCoroutineScheduler
import opensavvy.state.arrow.toEither
import opensavvy.state.outcome.Outcome

interface TestScope : Raise<Any> {

    val testJob: Job
        get() = testScope.coroutineContext[Job]
                ?: error("Couldn't find a job in the testScope")

    val testScope: CoroutineScope

    val backgroundJob: Job
        get() = backgroundScope.coroutineContext[Job]
            ?: error("Couldn't find a job in the backgroundScope")

    val backgroundScope: CoroutineScope

    val scheduler: TestCoroutineScheduler

    @RaiseDSL
    fun <T> Outcome<*, T>.bind() = this.toEither().bind()

}
