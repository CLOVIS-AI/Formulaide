package formulaide.server.routes

import formulaide.db.document.DbService
import formulaide.db.document.allServices
import formulaide.db.document.toApi
import formulaide.server.Auth.Companion.Employee
import formulaide.server.Auth.Companion.requireEmployee
import formulaide.server.database
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.response.*
import io.ktor.routing.*

fun Routing.serviceRoutes() {
	route("/services") {

		authenticate(Employee) {
			get("/list") {
				call.requireEmployee(database)

				val services = database.allServices()
					.map(DbService::toApi)

				call.respond(services)
			}
		}

	}

}
