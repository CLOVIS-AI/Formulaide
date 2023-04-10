package opensavvy.formulaide.remote.server

import kotlinx.coroutines.ExperimentalCoroutinesApi
import opensavvy.formulaide.fake.*
import opensavvy.formulaide.fake.spies.SpyDepartments.Companion.spied
import opensavvy.formulaide.remote.client.Records
import opensavvy.formulaide.remote.server.utils.TestClient
import opensavvy.formulaide.remote.server.utils.createTestServer
import opensavvy.formulaide.test.execution.Executor
import opensavvy.formulaide.test.execution.Suite
import opensavvy.formulaide.test.execution.prepare
import opensavvy.formulaide.test.execution.prepared
import opensavvy.formulaide.test.recordsTestSuite
import opensavvy.formulaide.test.utils.TestClock.Companion.testClock

class RemoteRecordTest : Executor() {

	@OptIn(ExperimentalCoroutinesApi::class)
	override fun Suite.register() {
		val testUsers by prepared { FakeUsers() }
		val testDepartments by prepared { FakeDepartments().spied() }
		val testForms by prepared { FakeForms(testClock()) }
		val testFiles by prepared { FakeFiles(testClock()) }

		val testRecords by prepared {
			val users = prepare(testUsers)
			val forms = prepare(testForms)
			val files = prepare(testFiles)
			val clock = testClock()

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
