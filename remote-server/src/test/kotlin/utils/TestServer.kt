package opensavvy.formulaide.remote.server.utils

import io.ktor.server.testing.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import opensavvy.formulaide.core.User
import opensavvy.formulaide.remote.server.configureServer

fun CoroutineScope.createTestServer(
	users: User.Service? = null,
	configure: TestApplicationBuilder.() -> Unit,
): TestApplication {
	val application = TestApplication {
		application {
			configureServer()
			configureTestAuthentication(users)
			configureTestLogging()
		}

		configure()
	}

	(coroutineContext[Job] ?: error("Couldn't find a job instance in this scope"))
		.invokeOnCompletion { application.stop() }

	return application
		.apply { start() }
}
