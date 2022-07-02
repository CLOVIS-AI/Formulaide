package formulaide.server.routes

import formulaide.db.document.getAlerts
import formulaide.server.Auth
import formulaide.server.Auth.Companion.requireAdmin
import formulaide.server.database
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Routing.alertRoutes() = route("/alerts") {
	authenticate(Auth.Employee) {
		get {
			call.requireAdmin(database)

			call.respond(database.getAlerts())
		}
	}
}
