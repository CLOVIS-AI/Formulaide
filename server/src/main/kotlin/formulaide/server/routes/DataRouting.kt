package formulaide.server.routes

import formulaide.api.data.NewCompoundData
import formulaide.db.document.createData
import formulaide.db.document.listData
import formulaide.server.Auth.Companion.Employee
import formulaide.server.Auth.Companion.requireAdmin
import formulaide.server.Auth.Companion.requireEmployee
import formulaide.server.database
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*

fun Routing.dataRoutes() {

	route("/data") {

		authenticate(Employee) {

			get("/list") {
				call.requireEmployee(database)

				call.respond(database.listData())
			}

			post("/create") {
				call.requireAdmin(database)

				val body = call.receive<NewCompoundData>()

				call.respond(database.createData(body))
			}

		}

	}

}
