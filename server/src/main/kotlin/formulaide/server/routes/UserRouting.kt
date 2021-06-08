package formulaide.server.routes

import formulaide.api.users.PasswordLogin
import formulaide.api.users.TokenResponse
import formulaide.server.Auth
import io.ktor.application.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*

fun Routing.userRoutes(auth: Auth) {
	route("/users") {

		post("/login") {
			val login = call.receive<PasswordLogin>()
			val (token, _) = auth.login(login)

			call.respond(TokenResponse(token))
		}

	}
}
