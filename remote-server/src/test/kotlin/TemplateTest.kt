package opensavvy.formulaide.remote.server

import kotlinx.coroutines.CoroutineScope
import opensavvy.formulaide.core.Template
import opensavvy.formulaide.fake.FakeTemplates
import opensavvy.formulaide.fake.spies.SpyTemplates.Companion.spied
import opensavvy.formulaide.remote.client.Templates
import opensavvy.formulaide.remote.server.utils.TestClient
import opensavvy.formulaide.remote.server.utils.createTestServer
import opensavvy.formulaide.server.templates
import opensavvy.formulaide.test.TemplateTestCases
import opensavvy.formulaide.test.utils.TestClock.Companion.testClock

class TemplateTest : TemplateTestCases() {

	override suspend fun new(
		foreground: CoroutineScope,
		background: CoroutineScope,
	): Template.Service {
		val clock = testClock()

		val application = background.createTestServer {
			routing {
				templates(FakeTemplates(clock).spied())
			}
		}

		val client = TestClient(application.client)

		return Templates(client, background.coroutineContext)
	}
}
