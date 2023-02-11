package opensavvy.formulaide.remote.server

import kotlinx.coroutines.ExperimentalCoroutinesApi
import opensavvy.formulaide.fake.FakeDepartments
import opensavvy.formulaide.fake.spies.SpyDepartments.Companion.spied
import opensavvy.formulaide.remote.client.Departments
import opensavvy.formulaide.remote.server.utils.TestClient
import opensavvy.formulaide.remote.server.utils.createTestServer
import opensavvy.formulaide.test.departmentTestSuite
import opensavvy.formulaide.test.execution.Executor
import opensavvy.formulaide.test.execution.Suite

class RemoteDepartmentTest : Executor() {

	@OptIn(ExperimentalCoroutinesApi::class)
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
