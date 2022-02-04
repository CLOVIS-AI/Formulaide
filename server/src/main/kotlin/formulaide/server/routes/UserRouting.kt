package formulaide.server.routes

import formulaide.api.users.*
import formulaide.db.document.*
import formulaide.server.Auth
import formulaide.server.Auth.Companion.Employee
import formulaide.server.Auth.Companion.requireAdmin
import formulaide.server.Auth.Companion.requireEmployee
import formulaide.server.allowUnsafeCookie
import formulaide.server.database
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.date.*

private fun ApplicationCall.setRefreshTokenCookie(value: String) {
	val extensions = HashMap<String, String?>()
	extensions["SameSite"] = "Strict"

	if (!this.application.developmentMode && !allowUnsafeCookie)
		extensions["Secure"] = null

	response.cookies.append(Cookie(
		"REFRESH-TOKEN",
		value = value,
		expires = GMTDate() + Auth.refreshTokenExpiration.toMillis(),
		httpOnly = true,
		path = "/",
		extensions = extensions,
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
				call.respondText("Les informations de connexion sont incorrectes. Veuillez attendre quelques secondes avant de réessayer.",
				                 status = HttpStatusCode.Forbidden)
			}
		}

		post("/logout") {
			call.setRefreshTokenCookie("NO COOKIE SET")
			call.respondText("You have been logged out.")
		}

		post("/refreshToken") {
			val refreshToken = call.request.cookies["REFRESH-TOKEN"]
				?: error("L'endpoint /users/refreshToken nécessite d'avoir le cookie REFRESH-TOKEN paramétré")

			val (accessToken, _) = auth.loginWithRefreshToken(refreshToken)

			call.setRefreshTokenCookie(refreshToken)
			call.respond(TokenResponse(accessToken))
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

				val oldPassword = data.oldPassword
				if (!oldPassword.isNullOrBlank()) {
					try {
						auth.login(PasswordLogin(email = user.email, password = oldPassword))
					} catch (e: Exception) {
						e.printStackTrace()
						call.respondText("Les informations de connexion sont incorrectes.",
						                 status = HttpStatusCode.Forbidden)
						return@post
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
					newService = data.service,
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
