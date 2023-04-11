package opensavvy.formulaide.remote.server

import opensavvy.formulaide.fake.FakeDepartments
import opensavvy.formulaide.fake.spies.SpyDepartments.Companion.spied
import opensavvy.formulaide.remote.client.Departments
import opensavvy.formulaide.remote.server.utils.TestClient
import opensavvy.formulaide.remote.server.utils.createTestServer
import opensavvy.formulaide.test.departmentTestSuite
import opensavvy.formulaide.test.structure.Suite
import opensavvy.formulaide.test.structure.TestExecutor

class RemoteDepartmentTest : TestExecutor() {

	override fun Suite.register() {
		departmentTestSuite {
			val application = backgroundScope.createTestServer {
				routing {
					departments(FakeDepartments().spied())
				}
			}

			Departments(
				TestClient(application.client),
				backgroundScope.coroutineContext,
			).spied()
		}
	}

}
