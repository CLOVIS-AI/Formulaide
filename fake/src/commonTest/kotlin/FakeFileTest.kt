package opensavvy.formulaide.fake

import opensavvy.formulaide.test.fileTestSuite
import opensavvy.formulaide.test.structure.*
import opensavvy.formulaide.test.utils.TestUsers.administratorAuth
import opensavvy.state.outcome.orThrow

class FakeFileTest : TestExecutor() {

	override fun Suite.register() {
		val testDepartment by prepared(administratorAuth) {
			val departments = FakeDepartments()
			departments.create("Test department").orThrow()
		}

		val testForms by prepared {
			FakeForms(clock)
		}

		val testFiles by prepared {
			FakeFiles(clock)
		}

		val testRecords by prepared {
			FakeRecords(clock, prepare(testFiles))
		}

		fileTestSuite(
			testDepartment,
			testForms,
			testRecords,
			testFiles,
		)
	}
}
