package formulaide.server.routes

import formulaide.api.data.FormSubmission
import formulaide.api.data.RecordsToReviewRequest
import formulaide.api.data.ReviewRequest
import formulaide.api.types.Ref
import formulaide.api.types.Ref.Companion.createRef
import formulaide.db.document.*
import formulaide.server.Auth
import formulaide.server.Auth.Companion.requireEmployee
import formulaide.server.database
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*

fun Routing.submissionRoutes() {
	route("/submissions") {

		post("/create") {
			val submission = call.receive<FormSubmission>()

			require(submission.root == null) { "L'endpoint /submissions/create ne peut être utilisé que pour les saisies originelles, pas pour la vérification." }

			val dbSubmission = database.saveSubmission(submission)
			database.createRecord(dbSubmission.toApi())

			call.respond("Success")
		}

		post("/nativeCreate/{formId}") {
			val formId =
				call.parameters["formId"] ?: error("Le paramètre GET 'formId' est obligatoire.")
			val form = database.findForm(formId)?.takeIf { it.public }
				?: error("Le formulaire demandé n'existe pas, ou n'est pas public : $formId")
			require(form.open) { "Le formulaire demandé a été archivé, il n'est plus possible d'y répondre." }

			val data = HashMap<String, String>()

			call.receiveMultipart().forEachPart {
				when (it) {
					is PartData.FormItem -> {
						data[it.name ?: error("La valeur '${it.value}' devrait avoir un nom.")] =
							it.value
					}
					else -> error("Le type de données '${it::class}' n'est pas supporté.")
				}

				it.dispose()
			}

			val submission = FormSubmission(
				id = Ref.SPECIAL_TOKEN_NEW,
				form = form.createRef(),
				root = null, // The raw HTML can only be used for the original submission
				data = data,
			)
			val dbSubmission = database.saveSubmission(submission)
			database.createRecord(dbSubmission.toApi())

			call.respondText("SUCCESS")
		}

		authenticate(Auth.Employee) {

			post("/review") {
				val employee = call.requireEmployee(database)
				val review = call.receive<ReviewRequest>()

				database.reviewRecord(review, employee)

				call.respond("Success")
			}

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

				val submissions =
					if (request.query.isNotEmpty()) request.query
						.map { (actionIdOrNull, query) -> actionIdOrNull?.let { actionId -> form.actions.find { it.id == actionId } } to query }
						.filter { (_, query) -> query.isNotEmpty() }
						.takeIf { it.isNotEmpty() } // If there are no criteria, ignore the request
						?.flatMap { (actionId, query) ->
							database.searchSubmission(
								form,
								actionId,
								query
							)
						}
					else null

				val records = database.findRecords(form, request.state, submissions)

				call.respond(records)
			}
		}
	}
}
