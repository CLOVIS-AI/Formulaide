package opensavvy.formulaide.remote.server.utils

import io.ktor.client.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
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
fun HttpClientConfig<*>.configureTestAuthentication() {
	install(Auth) {
		bearer {
			loadTokens {
				when (val auth = currentAuth()) {
					TestUsers.employeeAuth -> BearerTokens(auth.user!!.id, "none")
					TestUsers.administratorAuth -> BearerTokens(auth.user!!.id, "none")
					opensavvy.formulaide.core.Auth.Anonymous -> BearerTokens("guest", "none")
					else -> error("Cannot create a token for user $auth, this client is configured with the test authentication")
				}
			}
		}
	}
}
