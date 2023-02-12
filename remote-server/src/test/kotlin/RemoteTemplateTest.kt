package opensavvy.formulaide.remote.server

import kotlinx.coroutines.ExperimentalCoroutinesApi
import opensavvy.formulaide.fake.FakeTemplates
import opensavvy.formulaide.fake.spies.SpyTemplates.Companion.spied
import opensavvy.formulaide.remote.client.Templates
import opensavvy.formulaide.remote.server.utils.TestClient
import opensavvy.formulaide.remote.server.utils.createTestServer
import opensavvy.formulaide.test.execution.Executor
import opensavvy.formulaide.test.execution.Suite
import opensavvy.formulaide.test.templateTestSuite
import opensavvy.formulaide.test.utils.TestClock.Companion.testClock

class RemoteTemplateTest : Executor() {

	@OptIn(ExperimentalCoroutinesApi::class)
	override fun Suite.register() {
		templateTestSuite {
			val clock = testClock()

			val application = backgroundScope.createTestServer {
				routing {
					templates(FakeTemplates(clock).spied())
				}
			}

			Templates(
				TestClient(application.client),
				backgroundScope.coroutineContext,
			)
		}
	}

}
