package formulaide.server.routes

import formulaide.api.users.NewUser
import formulaide.api.users.PasswordLogin
import formulaide.api.users.TokenResponse
import formulaide.api.users.UserEdits
import formulaide.db.document.editUser
import formulaide.db.document.findUser
import formulaide.db.document.toApi
import formulaide.server.Auth
import formulaide.server.Auth.Companion.Employee
import formulaide.server.Auth.Companion.requireAdmin
import formulaide.server.Auth.Companion.requireEmployee
import formulaide.server.database
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*

fun Routing.userRoutes(auth: Auth) {
	route("/users") {

		post("/login") {
			val login = call.receive<PasswordLogin>()

			try {
				val (token, _) = auth.login(login)

				call.respond(TokenResponse(token))
			} catch (e: Exception) {
				e.printStackTrace()
				call.respondText("Les informations de connexion sont incorrectes.",
				                 status = HttpStatusCode.Forbidden)
			}
		}

		authenticate(Employee) {
			post("/create") {
				call.requireAdmin(database)
				val data = call.receive<NewUser>()

				val (token, _) = auth.newAccount(data)

				call.respond(TokenResponse(token))
			}

			post("/edit") {
				call.requireAdmin(database)
				val data = call.receive<UserEdits>()

				val user = database.findUser(data.user.email)
					?: error("Impossible de trouver un utilisateur ayant cette adresse mail")
				val editedUser = database.editUser(
					user,
					newEnabled = data.enabled
				)

				call.respond(editedUser.toApi())
			}

			get("/me") {
				val user = call.requireEmployee(database)

				call.respond(user.toApi())
			}
		}
	}
}
