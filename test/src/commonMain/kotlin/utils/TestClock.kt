package opensavvy.formulaide.test.utils

import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.coroutines.coroutineContext

class TestClock(private val scheduler: TestCoroutineScheduler) : Clock {
	override fun now(): Instant = Instant.fromEpochMilliseconds(scheduler.currentTime)

	companion object {
		suspend fun testClock(): TestClock {
			val scheduler = coroutineContext[TestCoroutineScheduler]
				?: error("No TestCoroutineScheduler is installed in this context: $coroutineContext")
			return TestClock(scheduler)
		}

		suspend fun currentInstant() = testClock().now()
	}
}
