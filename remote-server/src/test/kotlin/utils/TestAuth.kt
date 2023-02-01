package opensavvy.formulaide.remote.server.utils

import io.ktor.client.plugins.api.*
import io.ktor.client.request.*
import io.ktor.server.application.*
import kotlinx.coroutines.withContext
import opensavvy.formulaide.core.Auth
import opensavvy.formulaide.core.Auth.Companion.Guest
import opensavvy.formulaide.core.Auth.Companion.currentAuth
import opensavvy.formulaide.core.User
import opensavvy.formulaide.test.cases.TestUsers

// Server config
fun Application.configureTestAuthentication(additionalUsers: User.Service? = null) {
	log.error("THIS SERVER IS CONFIGURED USING THE TEST AUTHENTICATION. THIS IS NOT SAFE FOR PRODUCTION. IF YOU SEE THIS MESSAGE IN YOUR LOGS, CHECK YOUR CONFIGURATION IMMEDIATELY.")

	intercept(ApplicationCallPipeline.Setup) {
		// Authorization: Bearer <username>_<role>

		val bearer = context.request.headers["Authorization"]
			?.split(" ", limit = 2)
			?.get(1)
			?.split("_", limit = 2)

		if (bearer == null) {
			application.log.trace("No authorization header")
			proceed()
			return@intercept
		}

		val (id, role) = bearer
		val auth = when (id) {
			TestUsers.employeeAuth.user!!.id -> TestUsers.employeeAuth
			TestUsers.administratorAuth.user!!.id -> TestUsers.administratorAuth
			"guest" -> Guest
			else -> {
				if (additionalUsers != null) {
					Auth(User.Role.valueOf(role), User.Ref(id, additionalUsers))
				} else {
					error("The user '${id}' is not one of the test users. The server is currently configured to only authenticate test users.")
				}
			}
		}

		withContext(auth) {
			proceed()
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
			Guest -> "guest_${User.Role.Guest.name}"
			else -> "${auth.user!!.id}_${auth.role.name}"
		}

		request.header("Authorization", "Bearer $token")
	}
}
