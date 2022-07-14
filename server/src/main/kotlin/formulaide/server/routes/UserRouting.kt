package formulaide.server.routes

import formulaide.api.bones.*
import formulaide.db.document.*
import formulaide.server.Auth
import formulaide.server.Auth.Companion.Employee
import formulaide.server.Auth.Companion.requireAdmin
import formulaide.server.Auth.Companion.requireEmployee
import formulaide.server.database
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * The user management endpoint: `/api/users`.
 */
@Suppress("MemberVisibilityCanBePrivate")
object UserRouting {
	private lateinit var auth: Auth

	internal fun Routing.enable(auth: Auth): Route {
		this@UserRouting.auth = auth

		return route("/api/users") {
			create()
			id()
			list()
			me()
		}
	}

	/**
	 * The endpoint `/api/users/create`.
	 *
	 * ### Post
	 *
	 * Creates a new user.
	 *
	 * - Requires administrator authentication
	 * - Body: [ApiNewUser]
	 * - Response: `"Success"`
	 */
	fun Route.create() {
		authenticate(Employee) {
			post("/create") {
				call.requireAdmin(database)
				val data = call.receive<ApiNewUser>()

				auth.newAccount(data)

				call.respond("Success")
			}
		}
	}

	/**
	 * The endpoint `/api/users/me`.
	 *
	 * ### Get
	 *
	 * Gets the information of the currently logged-in user.
	 *
	 * - Requires employee authentication
	 * - Response: [ApiUser]
	 */
	fun Route.me() {
		authenticate(Employee) {
			get("/me") {
				val user = call.requireEmployee(database)
				call.respond(user.toApi())
			}
		}
	}

	/**
	 * The endpoint `/api/users`.
	 *
	 * ### Get
	 *
	 * Lists users.
	 *
	 * - Requires administrator authentication.
	 * - Optional parameter `closed`: set to `true` to also return disabled users.
	 * - Response: list of emails ([String])
	 */
	fun Route.list() {
		authenticate(Employee) {
			get {
				call.requireAdmin(database)

				val list = when (call.parameters["closed"].toBoolean()) {
					true -> database.listEnabledUsers()
					false -> database.listAllUsers()
				}

				call.respond(list.map { it.email })
			}
		}
	}

	/**
	 * The endpoint `/api/users/{email}`.
	 *
	 * ### Get
	 *
	 * Returns information about a specific user.
	 *
	 * - Requires administrator authentication.
	 * - Response: [ApiUser]
	 *
	 * ### Patch
	 *
	 * Edits information about a specific user.
	 *
	 * - Requires administrator authentication.
	 * - Body: [ApiUserEdition] (specify only the fields you want to edit)
	 * - Response: [ApiUser]
	 *
	 * ### Patch `/password`
	 *
	 * Edits the user's password.
	 *
	 * - Requires employee authentication.
	 * - Body: [ApiUserPasswordEdition]
	 *    - The field [ApiUserPasswordEdition.oldPassword] is mandatory for employees
	 *    - Employees can only edit their own account
	 * - Response: `"Success"`
	 *
	 * Editing the password invalidates all refresh tokens for the user.
	 * It is recommended to navigate the user to the log-in page after they edited their password.
	 */
	fun Route.id() {
		authenticate(Employee) {
			route("{email}") {
				get {
					call.requireAdmin(database)

					val email = call.parameters["email"] ?: error("Le paramètre obligatoire 'email' n'a pas été fourni")
					val user = database.findUser(email) ?: error("L'email fournie ne correspond à aucun utilisateur")

					call.respond(user.toApi())
				}

				patch {
					call.requireAdmin(database)
					val email = call.parameters["email"] ?: error("Le paramètre obligatoire 'email' n'a pas été fourni")
					val user = database.findUser(email) ?: error("L'email fournie ne correspond à aucun utilisateur")

					val data = call.receive<ApiUserEdition>()

					val editedUser = database.editUser(
						user,
						newEnabled = data.enabled,
						newIsAdministrator = data.administrator,
						newServices = data.departments,
					)

					call.respond(editedUser.toApi())
				}

				patch("/password") {
					val me = call.requireEmployee(database)
					val email = call.parameters["email"] ?: error("Le paramètre obligatoire 'email' n'a pas été fourni")
					require(me.email == email || me.isAdministrator) { "Seuls le propriétaire d'un compte et les administrateurs peuvent modifier un mot de passe" }

					val data = call.receive<ApiUserPasswordEdition>()
					val user = when (me.email == email) {
						true -> me
						false -> database.findUser(email) ?: error("L'email fournie ne correspond à aucun utilisateur")
					}

					val oldPassword = data.oldPassword
					if (!oldPassword.isNullOrBlank()) {
						try {
							auth.login(ApiPasswordLogin(email = user.email, password = oldPassword))
						} catch (e: Exception) {
							e.printStackTrace()
							call.respondText(
								"Les informations de connexion sont incorrectes.",
								status = HttpStatusCode.Forbidden
							)
							return@patch
						}
					} else {
						require(me.isAdministrator) { "Seul un administrateur peut modifier un mot de passe sans fournir sa valeur précédente" }
					}

					val newHashedPassword = Auth.hash(data.newPassword)
					database.editUserPassword(user, newHashedPassword)

					call.respond("Success")
				}
			}
		}
	}
}
