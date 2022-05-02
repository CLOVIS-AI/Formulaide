package formulaide.server.routes

import formulaide.api.users.ServiceModification
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

fun Routing.serviceRoutes() {
	route("/services") {

		authenticate(Employee) {
			get("/list") {
				call.requireEmployee(database)

				val services = database.allServices()
					.map(DbService::toApi)

				call.respond(services)
			}

			get("/fullList") {
				call.requireAdmin(database)

				val services = database.allServicesIgnoreOpen()
					.map(DbService::toApi)

				call.respond(services)
			}

			post("/create") {
				call.requireAdmin(database)

				val service = call.receive<String>().removeSurrounding("\"")
				val created = database.createService(service)

				call.respond(created.toApi())
			}

			post("/close") {
				call.requireAdmin(database)

				val service = call.receive<ServiceModification>()
				database.manageService(service.id.id.toInt(), service.open)

				call.respond(
					database.findService(service.id.id.toInt())?.toApi()
						?: error("Le service est introuvable alors qu'il a déjà été modifié, c'est impossible")
				)
			}
		}

	}

}
