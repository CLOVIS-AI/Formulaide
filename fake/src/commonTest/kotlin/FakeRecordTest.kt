package opensavvy.formulaide.fake

import kotlinx.coroutines.ExperimentalCoroutinesApi
import opensavvy.formulaide.fake.spies.SpyDepartments.Companion.spied
import opensavvy.formulaide.test.execution.Executor
import opensavvy.formulaide.test.execution.Suite
import opensavvy.formulaide.test.execution.prepare
import opensavvy.formulaide.test.execution.prepared
import opensavvy.formulaide.test.recordsTestSuite
import opensavvy.formulaide.test.utils.TestClock.Companion.testClock

class FakeRecordTest : Executor() {

	@OptIn(ExperimentalCoroutinesApi::class)
	override fun Suite.register() {
		val testDepartments by prepared { FakeDepartments().spied() }
		val testTemplates by prepared { FakeTemplates(testClock()) }
		val testForms by prepared { FakeForms(testClock()) }
		val testFiles by prepared { FakeFiles(testClock()) }
		val testRecords by prepared { FakeRecords(testClock(), prepare(testFiles)) }

		recordsTestSuite(
			testDepartments,
			testForms,
			testRecords,
			testFiles,
		)
	}

}
