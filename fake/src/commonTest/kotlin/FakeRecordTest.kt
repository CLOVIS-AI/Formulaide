package opensavvy.formulaide.fake

import opensavvy.formulaide.fake.spies.SpyDepartments.Companion.spied
import opensavvy.formulaide.fake.spies.SpyUsers.Companion.spied
import opensavvy.formulaide.fake.utils.commonIds
import opensavvy.formulaide.test.identifierParsingSuite
import opensavvy.formulaide.test.recordsTestSuite
import opensavvy.formulaide.test.structure.*

class FakeRecordTest : TestExecutor() {

	override fun Suite.register() {
		val testDepartments by prepared { FakeDepartments().spied() }
		val testUsers by prepared { FakeUsers().spied() }
		val testForms by prepared { FakeForms(clock) }
		val testFiles by prepared { FakeFiles(clock) }
		val testRecords by prepared { FakeRecords(clock, prepare(testFiles)) }
		val testSubmissions by prepared { prepare(testRecords).submissions }

		recordsTestSuite(
			testDepartments,
			testUsers,
			testForms,
			testRecords,
			testFiles,
		)

		identifierParsingSuite(
			testRecords,
			*commonIds,
		) { prepare(testRecords).Ref(it) }

		identifierParsingSuite(
			testSubmissions,
			*commonIds,
		) { prepare(testSubmissions).Ref(it) }
	}

}
