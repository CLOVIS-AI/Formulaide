package opensavvy.formulaide.backend

import io.ktor.server.application.*
import kotlinx.coroutines.withContext
import opensavvy.formulaide.core.Auth
import opensavvy.formulaide.core.User
import opensavvy.formulaide.core.User.Role.Companion.role
import opensavvy.formulaide.core.data.Token
import opensavvy.state.outcome.mapFailure
import opensavvy.state.outcome.value

class TokenAuthenticationConfig {
	lateinit var users: User.Service<*>
}

val TokenAuthentication = createApplicationPlugin(
	name = "FormulaideAuthentication",
	createConfiguration = ::TokenAuthenticationConfig,
) {
	val users = pluginConfig.users

	application.intercept(ApplicationCallPipeline.Setup) {
		// Authorization: Bearer <username>_<token>

		val bearer = context.request.headers["Authorization"]
			?.split(" ", limit = 2)
			?.get(1)
			?.split("_", limit = 2)

		if (bearer == null) {
			application.log.trace("No authorization header")
			proceed()
			return@intercept
		}

		val (id, token) = bearer
		application.log.trace("Found authorization header for user $id, checking if token is valid…")

		val user = users.fromIdentifier(id)

		user.verifyToken(Token(token))
			.mapFailure {
				application.log.warn("Could not check authorization token. $it")
				proceed()
				return@intercept
			}

		val role = getUserUnsafe(users, user)
			.mapFailure {
				application.log.warn("Could not find role for user $user, proceeding as guest; $it")
				proceed()
				return@intercept
			}
			.value
			.role

		withContext(Auth(role, user)) {
			application.log.trace("Valid token for $user")
			proceed()
		}
	}
}
