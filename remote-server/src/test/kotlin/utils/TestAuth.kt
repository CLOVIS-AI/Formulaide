package opensavvy.formulaide.remote.server.utils

import io.ktor.client.plugins.api.*
import io.ktor.client.request.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import opensavvy.formulaide.core.Auth.Companion.currentAuth
import opensavvy.formulaide.server.AuthPrincipal
import opensavvy.formulaide.test.cases.TestUsers

// Server config
fun Application.configureTestAuthentication() {
	log.error("THIS SERVER IS CONFIGURED USING THE TEST AUTHENTICATION. THIS IS NOT SAFE FOR PRODUCTION. IF YOU SEE THIS MESSAGE IN YOUR LOGS, CHECK YOUR CONFIGURATION IMMEDIATELY.")

	install(Authentication) {
		bearer {
			realm = "Formulaide 2.0"

			authenticate { credentials ->
				when (credentials.token) {
					TestUsers.employeeAuth.user!!.id -> AuthPrincipal(TestUsers.employeeAuth)
					TestUsers.administratorAuth.user!!.id -> AuthPrincipal(TestUsers.administratorAuth)
					"guest" -> null
					else -> {
						this@configureTestAuthentication.log.warn("The user '${credentials.token}' is not one of the test users. The server is currently configured to only authenticate test users.")
						null
					}
				}
			}
		}
	}
}

// Client config
/**
 * Unsafe implementation which takes the
 */
internal val LocalAuth = createClientPlugin(name = "LocalAuth") {
	onRequest { request, _ ->
		val token = when (val auth = currentAuth()) {
			TestUsers.employeeAuth -> auth.user!!.id
			TestUsers.administratorAuth -> auth.user!!.id
			opensavvy.formulaide.core.Auth.Anonymous -> "guest"
			else -> error("Cannot create a token for user $auth, this client is configured with the test authentication")
		}

		request.header("Authorization", "Bearer $token")
	}
}
