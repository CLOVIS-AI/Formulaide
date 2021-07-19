package formulaide.server.routes

import formulaide.api.data.Form
import formulaide.api.types.ReferenceId
import formulaide.db.document.createForm
import formulaide.db.document.findForm
import formulaide.db.document.listForms
import formulaide.db.document.referencedComposites
import formulaide.server.Auth.Companion.Employee
import formulaide.server.Auth.Companion.requireAdmin
import formulaide.server.Auth.Companion.requireEmployee
import formulaide.server.database
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*

fun Routing.formRoutes() {
	route("/forms") {

		get("/list") {
			call.respond(database.listForms(public = true))
		}

		post("/references") {
			val formId = call.receive<ReferenceId>().removeSurrounding("\"")
			val form = database.findForm(formId)
				?: error("Aucun formulaire ne correspond à l'identifiant $formId")

			call.respond(database.referencedComposites(form))
		}

		authenticate(Employee) {
			get("/listPublicInternal") {
				call.requireEmployee(database)
				call.respond(database.listForms(public = null))
			}

			get("/listClosed") {
				call.requireAdmin(database)
				call.respond(database.listForms(public = null, open = false))
			}

			post("/create") {
				call.requireAdmin(database)

				val form = call.receive<Form>()

				call.respond(database.createForm(form))
			}
		}
	}
}
