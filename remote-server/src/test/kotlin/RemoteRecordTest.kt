package opensavvy.formulaide.remote.server

import kotlinx.coroutines.ExperimentalCoroutinesApi
import opensavvy.formulaide.fake.*
import opensavvy.formulaide.fake.spies.SpyDepartments.Companion.spied
import opensavvy.formulaide.fake.spies.SpyForms.Companion.spied
import opensavvy.formulaide.fake.spies.SpyTemplates.Companion.spied
import opensavvy.formulaide.fake.spies.SpyUsers.Companion.spied
import opensavvy.formulaide.remote.client.Records
import opensavvy.formulaide.remote.server.utils.TestClient
import opensavvy.formulaide.remote.server.utils.createTestServer
import opensavvy.formulaide.test.RecordTestData
import opensavvy.formulaide.test.execution.Executor
import opensavvy.formulaide.test.execution.Suite
import opensavvy.formulaide.test.recordsTestSuite
import opensavvy.formulaide.test.utils.TestClock.Companion.testClock

class RemoteRecordTest : Executor() {

	@OptIn(ExperimentalCoroutinesApi::class)
	override fun Suite.register() {
		recordsTestSuite {
			val clock = testClock()
			val users = FakeUsers().spied()
			val departments = FakeDepartments().spied()
			val templates = FakeTemplates(clock).spied()
			val forms = FakeForms(clock).spied()
			val records = FakeRecords(clock)
			val submissions = records.submissions

			val application = backgroundScope.createTestServer {
				routing {
					records(users, forms, submissions, records)
				}
			}

			RecordTestData(
				departments,
				templates,
				forms,
				Records(
					TestClient(application.client),
					forms,
					users,
					backgroundScope.coroutineContext,
				)
			)
		}
	}
}
