package opensavvy.formulaide.remote.server.utils

import io.ktor.client.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import opensavvy.formulaide.core.Auth
import opensavvy.formulaide.core.currentAuth
import opensavvy.formulaide.remote.client.Client
import opensavvy.formulaide.remote.client.Client.Companion.configureClient
import opensavvy.logger.Logger.Companion.error
import opensavvy.logger.loggerFor

/**
 * A client implementation specialized for testing.
 *
 * It logs everything it does. It uses the authentication from the current coroutine context.
 */
class TestClient(client: HttpClient) : Client {

	private val log = loggerFor(this)

	override val http: HttpClient = client.config {
		configureClient()
		configureTestLogging()
		developmentMode = true

		log.error { "THIS CLIENT IS CONFIGURED USING THE TEST AUTHENTICATION. THIS IS NOT FIT FOR PRODUCTION. IF YOU SEE THIS MESSAGE IN YOUR LOGS, CHECK YOUR CONFIGURATION IMMEDIATELY." }

		install(LocalAuth)
	}

	override val auth: Flow<Auth>
		get() = flow { currentAuth() }

}
