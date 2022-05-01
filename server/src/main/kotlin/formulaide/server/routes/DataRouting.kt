package formulaide.server.routes

import formulaide.api.data.Composite
import formulaide.api.data.CompositeMetadata
import formulaide.db.document.createComposite
import formulaide.db.document.editComposite
import formulaide.db.document.listComposites
import formulaide.server.Auth.Companion.Employee
import formulaide.server.Auth.Companion.requireAdmin
import formulaide.server.Auth.Companion.requireEmployee
import formulaide.server.database
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Routing.dataRoutes() {

	route("/data") {

		authenticate(Employee) {

			get("/list") {
				call.requireEmployee(database)

				call.respond(database.listComposites())
			}

			post("/create") {
				call.requireAdmin(database)

				val body = call.receive<Composite>()

				call.respond(database.createComposite(body))
			}

			post("/editMetadata") {
				call.requireAdmin(database)

				val metadata = call.receive<CompositeMetadata>()
				database.editComposite(metadata.id, metadata.open)

				call.respondText("SUCCESS")
			}

		}

	}

}
