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
import opensavvy.formulaide.test.execution.Executor
import opensavvy.formulaide.test.execution.Suite
import opensavvy.formulaide.test.execution.prepare
import opensavvy.formulaide.test.execution.prepared
import opensavvy.formulaide.test.formTestSuite
import opensavvy.formulaide.test.utils.TestClock.Companion.testClock

class RemoteFormTest : Executor() {

	@OptIn(ExperimentalCoroutinesApi::class)
	override fun Suite.register() {
		val createDepartments by prepared { FakeDepartments().spied() }
		val createTemplates by prepared { FakeTemplates(testClock()).spied() }

		val createForms by prepared {
			val departments = prepare(createDepartments)
			val templates = prepare(createTemplates)
			val clock = testClock()

			val application = backgroundScope.createTestServer {
				routing {
					forms(departments, templates, FakeForms(clock).spied())
				}
			}

			Forms(
				TestClient(application.client),
				departments,
				templates,
				backgroundScope.coroutineContext,
			)
		}

		formTestSuite(createDepartments, createTemplates, createForms)
	}

}
