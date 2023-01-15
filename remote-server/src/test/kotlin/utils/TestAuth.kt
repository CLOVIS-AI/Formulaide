package opensavvy.formulaide.remote.server.utils

import io.ktor.client.plugins.api.*
import io.ktor.client.request.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import opensavvy.formulaide.core.Auth
import opensavvy.formulaide.core.Auth.Companion.Anonymous
import opensavvy.formulaide.core.Auth.Companion.currentAuth
import opensavvy.formulaide.core.User
import opensavvy.formulaide.server.AuthPrincipal
import opensavvy.formulaide.test.cases.TestUsers

// Server config
fun Application.configureTestAuthentication(additionalUsers: User.Service? = null) {
	log.error("THIS SERVER IS CONFIGURED USING THE TEST AUTHENTICATION. THIS IS NOT SAFE FOR PRODUCTION. IF YOU SEE THIS MESSAGE IN YOUR LOGS, CHECK YOUR CONFIGURATION IMMEDIATELY.")

	install(Authentication) {
		bearer {
			realm = "Formulaide 2.0"

			authenticate { credentials ->
				val (id, role) = credentials.token.split('_')
				when (id) {
					TestUsers.employeeAuth.user!!.id -> AuthPrincipal(TestUsers.employeeAuth)
					TestUsers.administratorAuth.user!!.id -> AuthPrincipal(TestUsers.administratorAuth)
					"guest" -> AuthPrincipal(Anonymous)
					else -> {
						if (additionalUsers != null) {
							AuthPrincipal(Auth(User.Role.valueOf(role), User.Ref(id, additionalUsers)))
						} else {
							error("The user '${credentials.token}' is not one of the test users. The server is currently configured to only authenticate test users.")
						}
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
			Anonymous -> "guest_${User.Role.Anonymous.name}"
			else -> "${auth.user!!.id}_${auth.role.name}"
		}

		request.header("Authorization", "Bearer $token")
	}
}
