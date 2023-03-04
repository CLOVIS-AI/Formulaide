package opensavvy.formulaide.fake

import kotlinx.coroutines.ExperimentalCoroutinesApi
import opensavvy.formulaide.fake.spies.SpyDepartments.Companion.spied
import opensavvy.formulaide.test.RecordTestData
import opensavvy.formulaide.test.execution.Executor
import opensavvy.formulaide.test.execution.Suite
import opensavvy.formulaide.test.recordsTestSuite
import opensavvy.formulaide.test.utils.TestClock.Companion.testClock

class FakeRecordTest : Executor() {

	@OptIn(ExperimentalCoroutinesApi::class)
	override fun Suite.register() {
		recordsTestSuite {
			val clock = testClock()

			RecordTestData(
				FakeDepartments().spied(),
				FakeTemplates(clock),
				FakeForms(clock),
				FakeRecords(clock, FakeFiles(clock)),
				FakeFiles(clock),
			)
		}
	}

}
