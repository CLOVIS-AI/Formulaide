package opensavvy.formulaide.fake

import kotlinx.coroutines.ExperimentalCoroutinesApi
import opensavvy.formulaide.test.execution.Executor
import opensavvy.formulaide.test.execution.Suite
import opensavvy.formulaide.test.execution.prepare
import opensavvy.formulaide.test.execution.prepared
import opensavvy.formulaide.test.fileTestSuite
import opensavvy.formulaide.test.utils.TestClock.Companion.testClock
import opensavvy.formulaide.test.utils.TestUsers.administratorAuth
import opensavvy.state.outcome.orThrow

class FakeFileTest : Executor() {

	@OptIn(ExperimentalCoroutinesApi::class)
	override fun Suite.register() {
		val testDepartment by prepared(administratorAuth) {
			val departments = FakeDepartments()
			departments.create("Test department").orThrow()
		}

		val testForms by prepared {
			FakeForms(testClock())
		}

		val testFiles by prepared {
			FakeFiles(testClock())
		}

		val testRecords by prepared {
			FakeRecords(testClock(), prepare(testFiles))
		}

		fileTestSuite(
			testDepartment,
			testForms,
			testRecords,
			testFiles,
		)
	}
}
