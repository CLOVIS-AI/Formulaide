package opensavvy.formulaide.fake

import kotlinx.coroutines.ExperimentalCoroutinesApi
import opensavvy.formulaide.test.FileTestData
import opensavvy.formulaide.test.execution.Executor
import opensavvy.formulaide.test.execution.Suite
import opensavvy.formulaide.test.fileTestSuite
import opensavvy.formulaide.test.utils.TestClock.Companion.testClock

class FakeFileTest : Executor() {

	@OptIn(ExperimentalCoroutinesApi::class)
	override fun Suite.register() {
		fileTestSuite {
			FileTestData(
				FakeFiles(testClock())
			)
		}
	}
}
