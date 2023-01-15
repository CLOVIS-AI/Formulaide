package opensavvy.formulaide.remote.server

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.debug.junit4.CoroutinesTimeout
import opensavvy.formulaide.core.Department
import opensavvy.formulaide.fake.FakeDepartments
import opensavvy.formulaide.fake.spies.SpyDepartments.Companion.spied
import opensavvy.formulaide.remote.client.Departments
import opensavvy.formulaide.remote.server.utils.TestClient
import opensavvy.formulaide.remote.server.utils.createTestServer
import opensavvy.formulaide.server.departments
import opensavvy.formulaide.test.DepartmentTestCases
import org.junit.Rule

class DepartmentTest : DepartmentTestCases() {

	@get:Rule
	val timeout = CoroutinesTimeout.seconds(15)

	override suspend fun new(
		foreground: CoroutineScope,
		background: CoroutineScope,
	): Department.Service {
		val application = background.createTestServer {
			routing {
				departments(FakeDepartments().spied())
			}
		}

		return Departments(
			TestClient(application.client),
			background.coroutineContext,
		).spied()
	}

}
