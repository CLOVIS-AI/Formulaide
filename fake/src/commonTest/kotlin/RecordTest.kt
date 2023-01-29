package opensavvy.formulaide.fake

import kotlinx.coroutines.CoroutineScope
import opensavvy.formulaide.core.Record
import opensavvy.formulaide.test.RecordTestCases
import opensavvy.formulaide.test.utils.TestClock.Companion.testClock

class RecordTest : RecordTestCases() {
	override suspend fun new(
		foreground: CoroutineScope,
		background: CoroutineScope,
	): Record.Service = FakeRecords(testClock())

}
