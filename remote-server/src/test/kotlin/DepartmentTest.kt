package opensavvy.formulaide.remote.server

import io.ktor.server.testing.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.debug.junit4.CoroutinesTimeout
import opensavvy.formulaide.core.Department
import opensavvy.formulaide.fake.FakeDepartments
import opensavvy.formulaide.fake.spies.SpyDepartments.Companion.spied
import opensavvy.formulaide.remote.client.Client
import opensavvy.formulaide.remote.client.Client.Companion.configureClient
import opensavvy.formulaide.remote.client.Departments
import opensavvy.formulaide.remote.server.utils.configureTestAuthentication
import opensavvy.formulaide.remote.server.utils.configureTestLogging
import opensavvy.formulaide.server.configureServer
import opensavvy.formulaide.server.departments
import opensavvy.formulaide.test.DepartmentTestCases
import org.junit.Rule
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

class DepartmentTest : DepartmentTestCases() {

	@get:Rule
	val timeout = CoroutinesTimeout.seconds(15)

	private lateinit var application: TestApplication

	@BeforeTest
	fun start() {
		application = TestApplication {
			application {
				configureServer()
				configureTestAuthentication()
				configureTestLogging()
			}

			routing {
				departments(FakeDepartments().spied())
			}
		}
		application.start()
	}

	@AfterTest
	fun stop() {
		application.stop()
	}

	override suspend fun new(
		foreground: CoroutineScope,
		background: CoroutineScope,
	): Department.Service {
		val client = application.createClient {
			configureClient()
			configureTestAuthentication()
			configureTestLogging()
		}.let { Client(it) }

		return Departments(client, background.coroutineContext).spied()
	}

}
