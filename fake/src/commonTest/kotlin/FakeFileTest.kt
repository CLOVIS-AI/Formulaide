package opensavvy.formulaide.fake

import opensavvy.formulaide.fake.utils.commonIds
import opensavvy.formulaide.test.fileTestSuite
import opensavvy.formulaide.test.identifierParsingSuite
import opensavvy.formulaide.test.structure.*
import opensavvy.formulaide.test.utils.TestUsers.administratorAuth

class FakeFileTest : TestExecutor() {

	override fun Suite.register() {
		val testDepartment by prepared(administratorAuth) {
			val departments = FakeDepartments()
			departments.create("Test department").bind()
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

		identifierParsingSuite(
			testFiles,
			*commonIds,
		) { prepare(testFiles).Ref(it) }
	}
}
