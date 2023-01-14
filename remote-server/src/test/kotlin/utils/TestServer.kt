package opensavvy.formulaide.remote.server.utils

import io.ktor.server.testing.*
import opensavvy.formulaide.core.User
import opensavvy.formulaide.server.configureServer
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

interface TestServer {

	var application: TestApplication

	fun TestApplicationBuilder.configureTestServer()

	val userService: User.Service? get() = null

	@BeforeTest
	fun startTestServer() {
		application = TestApplication {
			application {
				configureServer()
				configureTestAuthentication(userService)
				configureTestLogging()
			}

			configureTestServer()
		}
		application.start()
	}

	@AfterTest
	fun stopTestServer() {
		application.stop()
	}

}
