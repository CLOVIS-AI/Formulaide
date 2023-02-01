package opensavvy.formulaide.backend

import arrow.core.getOrHandle
import io.ktor.server.application.*
import kotlinx.coroutines.withContext
import opensavvy.backbone.Ref.Companion.now
import opensavvy.formulaide.core.Auth
import opensavvy.formulaide.core.User
import opensavvy.formulaide.core.User.Role.Companion.role
import opensavvy.formulaide.core.data.Token

class TokenAuthenticationConfig {
	lateinit var users: User.Service
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

		val user = User.Ref(id, users)

		user.verifyToken(Token(token))
			.getOrHandle {
				application.log.warn("Could not check authorization token. $it")
				proceed()
				return@intercept
			}

		val role = user.now()
			.getOrHandle {
				application.log.warn("Could not find role for user $user, proceeding as guest; $it")
				proceed()
				return@intercept
			}
			.role

		withContext(Auth(role, user)) {
			application.log.trace("Valid token for $user")
			proceed()
		}
	}
}
