package opensavvy.formulaide.remote.server

import kotlinx.coroutines.ExperimentalCoroutinesApi
import opensavvy.formulaide.fake.FakeDepartments
import opensavvy.formulaide.fake.FakeForms
import opensavvy.formulaide.fake.FakeTemplates
import opensavvy.formulaide.fake.spies.SpyDepartments.Companion.spied
import opensavvy.formulaide.fake.spies.SpyForms.Companion.spied
import opensavvy.formulaide.fake.spies.SpyTemplates.Companion.spied
import opensavvy.formulaide.remote.client.Forms
import opensavvy.formulaide.remote.server.utils.TestClient
import opensavvy.formulaide.remote.server.utils.createTestServer
import opensavvy.formulaide.test.FormTestData
import opensavvy.formulaide.test.execution.Executor
import opensavvy.formulaide.test.execution.Suite
import opensavvy.formulaide.test.formTestSuite
import opensavvy.formulaide.test.utils.TestClock.Companion.testClock

class RemoteFormTest : Executor() {

	@OptIn(ExperimentalCoroutinesApi::class)
	override fun Suite.register() {
		formTestSuite {
			val clock = testClock()
			val departments = FakeDepartments().spied()
			val templates = FakeTemplates(clock).spied()

			val application = backgroundScope.createTestServer {
				routing {
					forms(departments, templates, FakeForms(clock).spied())
				}
			}

			FormTestData(
				departments,
				templates,
				Forms(
					TestClient(application.client),
					departments,
					templates,
					backgroundScope.coroutineContext,
				)
			)
		}
	}

}
