package opensavvy.formulaide.remote.server

import opensavvy.formulaide.fake.*
import opensavvy.formulaide.fake.spies.SpyDepartments.Companion.spied
import opensavvy.formulaide.remote.client.Records
import opensavvy.formulaide.remote.server.utils.TestClient
import opensavvy.formulaide.remote.server.utils.createTestServer
import opensavvy.formulaide.test.recordsTestSuite
import opensavvy.formulaide.test.structure.*

class RemoteRecordTest : TestExecutor() {

	override fun Suite.register() {
		val testUsers by prepared { FakeUsers() }
		val testDepartments by prepared { FakeDepartments().spied() }
		val testForms by prepared { FakeForms(clock) }
		val testFiles by prepared { FakeFiles(clock) }

		val testRecords by prepared {
			val users = prepare(testUsers)
			val forms = prepare(testForms)
			val files = prepare(testFiles)

			val records = FakeRecords(clock, files)

			val application = backgroundScope.createTestServer {
				routing {
					records(users, forms, records.submissions, records)
				}
			}

			Records(
				TestClient(application.client),
				forms,
				users,
				backgroundScope.coroutineContext,
			)
		}

		recordsTestSuite(
			testDepartments,
			testForms,
			testRecords,
			testFiles,
		)
	}
}
