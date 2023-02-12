package opensavvy.formulaide.fake

import kotlinx.coroutines.ExperimentalCoroutinesApi
import opensavvy.formulaide.fake.spies.SpyDepartments.Companion.spied
import opensavvy.formulaide.test.FormTestData
import opensavvy.formulaide.test.execution.Executor
import opensavvy.formulaide.test.execution.Suite
import opensavvy.formulaide.test.formTestSuite
import opensavvy.formulaide.test.utils.TestClock.Companion.testClock

class FakeFormTest : Executor() {

	@OptIn(ExperimentalCoroutinesApi::class)
	override fun Suite.register() {
		formTestSuite {
			FormTestData(
				FakeDepartments().spied(),
				FakeTemplates(testClock()),
				FakeForms(testClock()),
			)
		}
	}

}
