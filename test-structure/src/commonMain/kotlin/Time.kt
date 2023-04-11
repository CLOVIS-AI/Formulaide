package opensavvy.formulaide.test.structure

import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

data class TestClock(private val scheduler: TestCoroutineScheduler) : Clock {
    override fun now() =
        Instant.fromEpochMilliseconds(scheduler.currentTime)
}

val TestScope.clock get() = TestClock(scheduler)

@OptIn(ExperimentalTime::class)
val TestScope.timeSource get() = scheduler.timeSource

val TestScope.currentTime get() = scheduler.currentTime

val TestScope.currentInstant get() = clock.now()

fun TestScope.advanceTimeBy(millis: Long) = scheduler.advanceTimeBy(millis)

fun TestScope.advanceTimeBy(duration: Duration) = scheduler.advanceTimeBy(duration.inWholeMilliseconds)

fun TestScope.advanceUntilIdle() = scheduler.advanceUntilIdle()
