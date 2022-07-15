package formulaide.server.routes

import formulaide.api.bones.ApiDepartment
import formulaide.core.Department
import formulaide.db.document.*
import formulaide.server.Auth.Companion.Employee
import formulaide.server.Auth.Companion.requireAdmin
import formulaide.server.Auth.Companion.requireEmployee
import formulaide.server.database
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Department management.
 *
 * This endpoint lives at `/api/departments` and is used to manage [company departments][Department].
 */
@Suppress("MemberVisibilityCanBePrivate")
object DepartmentRouting {

	internal fun Routing.enable() = route("/api/departments") {
		list()
		id()
		create()
	}

	/**
	 * Endpoint `/api/departments/list`.
	 *
	 * ### Get
	 *
	 * Lists the available departments.
	 *
	 * - Requires employee authentication
	 * - To include closed departments, set the `closed` optional parameter to `true` (requires administrator authentication)
	 * - Response: list of department IDs ([Int], see [id])
	 */
	fun Route.list() {
		authenticate(Employee) {
			get("/list") {
				val services = when (call.parameters["closed"].toBoolean()) {
					true -> {
						call.requireAdmin(database)
						database.allServicesIgnoreOpen()
					}

					false -> {
						call.requireEmployee(database)
						database.allServices()
					}
				}

				call.respond(services.map { it.id })
			}
		}
	}

	/**
	 * Endpoint `/api/departments/{id}`.
	 *
	 * ### Get
	 *
	 * Gets all information about a specific department.
	 *
	 * - Requires employee authentication if the department is [open][Department.open]
	 * - Requires administrator authentication if the department is [closed][Department.open]
	 * - Response: [ApiDepartment]
	 *
	 * ### Patch
	 *
	 * Edits a department.
	 *
	 * - Requires administrator authentication
	 * - Optional parameter `open`:
	 *    - `true`: opens the department
	 *    - `false`: closes the department
	 *    - `null` or omitted: no changes
	 * - Response: `"Success"`
	 */
	fun Route.id() {
		authenticate(Employee) {
			get("/{id}") {
				val id = call.parameters["id"] ?: error("Missing parameter 'id'")

				val service = database.findService(id.toInt())

				if (service == null || !service.open) {
					call.requireAdmin(database)

					requireNotNull(service) { "Service could not be found: $id" }
				} else {
					call.requireEmployee(database)
				}

				call.respond(service.toApi())
			}

			patch("/{id}") {
				call.requireAdmin(database)

				val id = call.parameters["id"] ?: error("Missing parameter 'id'")
				val open = call.parameters["open"]?.toBooleanStrictOrNull()
				if (open != null)
					database.manageService(id.toInt(), open)

				call.respond("Success")
			}
		}
	}

	/**
	 * Endpoint `/api/departments/create`
	 *
	 * ### Post
	 *
	 * Creates a new department.
	 *
	 * - Requires administrator authentication
	 * - The request body should be the name of the department ([String])
	 * - Response: [ApiDepartment]
	 */
	fun Route.create() {
		authenticate(Employee) {
			post("/create") {
				call.requireAdmin(database)

				val service = call.receive<String>().removeSurrounding("\"")
				val created = database.createService(service)

				call.respond(created.toApi())
			}
		}
	}
}
