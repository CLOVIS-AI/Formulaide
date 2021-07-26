package formulaide.server.routes

import formulaide.api.data.FormSubmission
import formulaide.api.data.RecordsToReviewRequest
import formulaide.db.document.*
import formulaide.server.Auth
import formulaide.server.Auth.Companion.requireEmployee
import formulaide.server.database
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*

fun Routing.submissionRoutes() {
	route("submissions") {

		post("/create") {
			val submission = call.receive<FormSubmission>()

			val dbSubmission = database.saveSubmission(submission)
			database.createRecord(dbSubmission.toApi())

			call.respond("Success")
		}

		authenticate(Auth.Employee) {

			post("/get") {
				call.requireEmployee(database)
				val body = call.receive<String>().removeSurrounding("\"")
				call.respond(database.findSubmissionById(body)?.toApi()
					             ?: error("La saisie '$body' est introuvable"))
			}

			get("/formsToReview") {
				val user = call.requireEmployee(database)
				val forms = database.findFormsAssignedTo(user)
				call.respond(forms)
			}

			post("/recordsToReview") {
				call.requireEmployee(database)
				val request = call.receive<RecordsToReviewRequest>()
				val form = database.findForm(request.form.id)
					?: error("Le formulaire est introuvable : ${request.form.id}")

				//TODO audit: refuse access to the records based on some rights?

				val records = database.findRecords(form, request.state)

				call.respond(records)
			}
		}
	}
}
