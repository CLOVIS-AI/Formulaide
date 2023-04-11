package opensavvy.formulaide.fake

import opensavvy.formulaide.fake.spies.SpyDepartments.Companion.spied
import opensavvy.formulaide.test.recordsTestSuite
import opensavvy.formulaide.test.structure.*

class FakeRecordTest : TestExecutor() {

	override fun Suite.register() {
		val testDepartments by prepared { FakeDepartments().spied() }
		val testForms by prepared { FakeForms(clock) }
		val testFiles by prepared { FakeFiles(clock) }
		val testRecords by prepared { FakeRecords(clock, prepare(testFiles)) }

		recordsTestSuite(
			testDepartments,
			testForms,
			testRecords,
			testFiles,
		)
	}

}
