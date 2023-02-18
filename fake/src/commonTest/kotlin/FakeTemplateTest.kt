package opensavvy.formulaide.fake

import kotlinx.coroutines.ExperimentalCoroutinesApi
import opensavvy.formulaide.test.execution.Executor
import opensavvy.formulaide.test.execution.Suite
import opensavvy.formulaide.test.templateTestSuite
import opensavvy.formulaide.test.utils.TestClock.Companion.testClock

class FakeTemplateTest : Executor() {

	@OptIn(ExperimentalCoroutinesApi::class)
	override fun Suite.register() {
		templateTestSuite { FakeTemplates(testClock()) }
	}

}
