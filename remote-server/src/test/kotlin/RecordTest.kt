package opensavvy.formulaide.remote.server

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.debug.junit4.CoroutinesTimeout
import opensavvy.formulaide.core.Record
import opensavvy.formulaide.fake.FakeForms
import opensavvy.formulaide.fake.FakeRecords
import opensavvy.formulaide.fake.FakeUsers
import opensavvy.formulaide.fake.spies.SpyForms.Companion.spied
import opensavvy.formulaide.fake.spies.SpyUsers.Companion.spied
import opensavvy.formulaide.remote.client.Records
import opensavvy.formulaide.remote.server.utils.TestClient
import opensavvy.formulaide.remote.server.utils.createTestServer
import opensavvy.formulaide.test.RecordTestCases
import opensavvy.formulaide.test.utils.TestClock.Companion.testClock
import org.junit.Rule

class RecordTest : RecordTestCases() {

	@get:Rule
	val timeout = CoroutinesTimeout.seconds(15)

	override suspend fun new(
		foreground: CoroutineScope,
		background: CoroutineScope,
	): Record.Service {
		val clock = testClock()

		val users = FakeUsers().spied()
		val forms = FakeForms(clock).spied()
		val records = FakeRecords(clock)
		val submissions = records.submissions

		val application = background.createTestServer {
			routing {
				records(
					users,
					forms,
					submissions,
					records,
				)
			}
		}

		return Records(
			TestClient(application.client),
			forms,
			users,
			background.coroutineContext,
		)
	}

}
