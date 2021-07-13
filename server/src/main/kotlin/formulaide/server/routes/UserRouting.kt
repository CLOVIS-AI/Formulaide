package formulaide.server.routes

import formulaide.api.users.*
import formulaide.db.document.*
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
import io.ktor.util.date.*

private fun ApplicationCall.setRefreshTokenCookie(value: String) {
	response.cookies.append(Cookie(
		"REFRESH-TOKEN",
		value = value,
		expires = GMTDate() + Auth.refreshTokenExpiration.toMillis(),
		secure = !this.application.developmentMode,
		httpOnly = true,
		extensions = mapOf(
			"SameSite" to "Strict",
		)
	))
}

fun Routing.userRoutes(auth: Auth) {
	route("/users") {

		post("/login") {
			val login = call.receive<PasswordLogin>()

			try {
				val (accessToken, refreshToken, _) = auth.login(login)

				call.setRefreshTokenCookie(refreshToken)
				call.respond(TokenResponse(accessToken))
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

			post("/password") {
				val me = call.requireEmployee(database)
				val data = call.receive<PasswordEdit>()
				require(me.email == data.user.email || me.isAdministrator) { "Seul un administrateur, ou la personne concernée, peuvent modifier un mot de passe" }

				val user =
					if (me.email == data.user.email) me
					else database.findUser(data.user.email)
						?: error("Aucun utilisateur ne correspond à l'adresse mail ${data.user.email}")

				if (data.oldPassword != null) {
					val oldPassword = data.oldPassword!!

					try {
						auth.login(PasswordLogin(email = user.email, password = oldPassword))
					} catch (e: Exception) {
						e.printStackTrace()
						call.respondText("Les informations de connexion sont incorrectes.",
						                 status = HttpStatusCode.Forbidden)
					}
				} else {
					require(me.isAdministrator) { "Seul un administrateur peut modifier un mot de passe sans fournir sa valeur précédente" }
				}

				val newHashedPassword = Auth.hash(data.newPassword)
				database.editUserPassword(user, newHashedPassword)

				call.respondText("Le mot de passe a été modifié.")
			}

			post("/edit") {
				call.requireAdmin(database)
				val data = call.receive<UserEdits>()

				val user = database.findUser(data.user.email)
					?: error("Impossible de trouver un utilisateur ayant cette adresse mail")
				val editedUser = database.editUser(
					user,
					newEnabled = data.enabled,
					newIsAdministrator = data.administrator,
				)

				call.respond(editedUser.toApi())
			}

			get("/listEnabled") {
				call.requireAdmin(database)
				call.respond(database.listEnabledUsers().map { it.toApi() })
			}

			get("/listAll") {
				call.requireAdmin(database)
				call.respond(database.listAllUsers().map { it.toApi() })
			}

			get("/me") {
				val user = call.requireEmployee(database)

				call.respond(user.toApi())
			}
		}
	}
}
