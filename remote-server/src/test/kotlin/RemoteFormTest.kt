package opensavvy.formulaide.remote.server

import opensavvy.formulaide.fake.FakeDepartments
import opensavvy.formulaide.fake.FakeForms
import opensavvy.formulaide.fake.FakeTemplates
import opensavvy.formulaide.fake.spies.SpyDepartments.Companion.spied
import opensavvy.formulaide.fake.spies.SpyForms.Companion.spied
import opensavvy.formulaide.fake.spies.SpyTemplates.Companion.spied
import opensavvy.formulaide.remote.client.Forms
import opensavvy.formulaide.remote.server.utils.TestClient
import opensavvy.formulaide.remote.server.utils.createTestServer
import opensavvy.formulaide.test.formTestSuite
import opensavvy.formulaide.test.structure.*

class RemoteFormTest : TestExecutor() {

	override fun Suite.register() {
		val createDepartments by prepared { FakeDepartments().spied() }
		val createTemplates by prepared { FakeTemplates(clock).spied() }

		val createForms by prepared {
			val departments = prepare(createDepartments)
			val templates = prepare(createTemplates)
			val clock = clock

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
