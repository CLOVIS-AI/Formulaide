package opensavvy.formulaide.remote.server

import kotlinx.coroutines.CoroutineScope
import opensavvy.formulaide.core.Form
import opensavvy.formulaide.fake.FakeDepartments
import opensavvy.formulaide.fake.FakeForms
import opensavvy.formulaide.fake.FakeTemplates
import opensavvy.formulaide.fake.spies.SpyDepartments.Companion.spied
import opensavvy.formulaide.fake.spies.SpyForms.Companion.spied
import opensavvy.formulaide.fake.spies.SpyTemplates.Companion.spied
import opensavvy.formulaide.remote.client.Forms
import opensavvy.formulaide.remote.server.utils.TestClient
import opensavvy.formulaide.remote.server.utils.createTestServer
import opensavvy.formulaide.server.forms
import opensavvy.formulaide.test.FormTestCases
import opensavvy.formulaide.test.utils.TestClock.Companion.testClock

class FormTest : FormTestCases() {

	override suspend fun new(
		foreground: CoroutineScope,
		background: CoroutineScope,
	): Form.Service {
		val clock = testClock()
		val departments = FakeDepartments().spied()
		val templates = FakeTemplates(clock).spied()

		val application = background.createTestServer {
			routing {
				forms(
					departments,
					templates,
					FakeForms(clock).spied(),
				)
			}
		}

		val client = TestClient(application.client)

		return Forms(client, departments, templates, background.coroutineContext)
	}
}
