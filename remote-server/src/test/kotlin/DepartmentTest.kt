package opensavvy.formulaide.remote.server

import io.ktor.server.testing.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.debug.junit4.CoroutinesTimeout
import opensavvy.formulaide.core.Department
import opensavvy.formulaide.fake.FakeDepartments
import opensavvy.formulaide.fake.spies.SpyDepartments.Companion.spied
import opensavvy.formulaide.remote.client.Departments
import opensavvy.formulaide.remote.server.utils.TestClient
import opensavvy.formulaide.remote.server.utils.TestServer
import opensavvy.formulaide.server.departments
import opensavvy.formulaide.test.DepartmentTestCases
import org.junit.Rule

class DepartmentTest : DepartmentTestCases(), TestServer {

	@get:Rule
	val timeout = CoroutinesTimeout.seconds(15)

	override lateinit var application: TestApplication

	override fun TestApplicationBuilder.configureTestServer() {
		routing {
			departments(FakeDepartments().spied())
		}
	}

	override suspend fun new(
		foreground: CoroutineScope,
		background: CoroutineScope,
	): Department.Service = Departments(
		TestClient(application.client),
		background.coroutineContext,
	).spied()

}
