package opensavvy.formulaide.remote.server

import opensavvy.formulaide.fake.FakeTemplates
import opensavvy.formulaide.fake.spies.SpyTemplates.Companion.spied
import opensavvy.formulaide.remote.client.RemoteTemplates
import opensavvy.formulaide.remote.server.utils.TestClient
import opensavvy.formulaide.remote.server.utils.createTestServer
import opensavvy.formulaide.test.structure.Suite
import opensavvy.formulaide.test.structure.TestExecutor
import opensavvy.formulaide.test.structure.clock
import opensavvy.formulaide.test.structure.prepared
import opensavvy.formulaide.test.templateTestSuite

class RemoteTemplateTest : TestExecutor() {

	override fun Suite.register() {
		val templates by prepared {
			val application = backgroundScope.createTestServer {
				routing {
					templates(FakeTemplates(clock).spied())
				}
			}

			RemoteTemplates(
				TestClient(application.client),
				backgroundScope,
			)
		}

		templateTestSuite(templates)
	}

}
