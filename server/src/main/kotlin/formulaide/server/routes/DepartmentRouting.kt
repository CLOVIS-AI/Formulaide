package formulaide.server.routes

import formulaide.core.Department
import formulaide.server.Auth.Companion.Employee
import formulaide.server.Auth.Companion.requireAdmin
import formulaide.server.Auth.Companion.requireEmployee
import formulaide.server.database
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import opensavvy.backbone.Ref.Companion.requestValue

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
						database.departments.all(includeClosed = true)
					}

					false -> {
						call.requireEmployee(database)
						database.departments.all(includeClosed = false)
					}
				}

				call.respond(services)
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
	 * - Response: [Department]
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

				val service = database.departments.fromId(id.toInt()).requestValue()

				if (!service.open) {
					call.requireAdmin(database)
				} else {
					call.requireEmployee(database)
				}

				call.respond(service)
			}

			patch("/{id}") {
				call.requireAdmin(database)

				val id = call.parameters["id"] ?: error("Missing parameter 'id'")
				val open = call.parameters["open"]?.toBooleanStrictOrNull()
				if (open == true)
					database.departments.open(database.departments.fromId(id.toInt()))
				if (open == false)
					database.departments.close(database.departments.fromId(id.toInt()))

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
	 * - Response: [Department]
	 */
	fun Route.create() {
		authenticate(Employee) {
			post("/create") {
				call.requireAdmin(database)

				val service = call.receive<String>().removeSurrounding("\"")
				val created = database.departments.create(service)

				call.respond(created)
			}
		}
	}
}
