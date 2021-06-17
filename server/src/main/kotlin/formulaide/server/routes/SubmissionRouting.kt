package formulaide.server.routes

import formulaide.api.data.FormSubmission
import formulaide.db.document.saveSubmission
import formulaide.server.database
import io.ktor.application.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*

fun Routing.submissionRoutes() {
	route("submissions") {

		post("/create") {
			val body = call.receive<FormSubmission>()
			database.saveSubmission(body)
			call.respond("Success")
		}

	}
}
