package formulaide.server.routes

import formulaide.api.bones.ApiNewRecord
import formulaide.api.bones.ApiRecordReview
import formulaide.core.form.Form
import formulaide.core.form.Submission
import formulaide.core.record.Record
import formulaide.server.Auth
import formulaide.server.Auth.Companion.Employee
import formulaide.server.database
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import opensavvy.backbone.Ref.Companion.requestValue
import opensavvy.backbone.Ref.Companion.requestValueOrThrow

/**
 * The record and submission management endpoint: `/api/records`.
 */
@Suppress("MemberVisibilityCanBePrivate")
object RecordRouting {

	internal fun Routing.enable() = route("/api/records") {
		records()
		record()
	}

	/**
	 * General methods to deal with records: `/api/records`.
	 *
	 * ### Get
	 *
	 * Requests the list of all records.
	 * In the future, this endpoint will allow to declare search criteria to query the database.
	 *
	 * - Requires employee authentication
	 * - Response: list of record identifiers ([String])
	 *
	 * ### Post
	 *
	 * Creates a new record.
	 * This method is called when a user fills in a [Form].
	 *
	 * - Body: [ApiNewRecord]
	 * - Response: identifier of the created record ([String])
	 */
	fun Route.records() {
		authenticate(Employee, optional = true) {
			post {
				val body = call.receive<ApiNewRecord>()
				val user = call.principal<Auth.AuthPrincipal>()
					?.let { database.users.fromId(it.email.email) }

				val id = database.records.create(
					body.form,
					body.version,
					user,
					body.submission,
				)

				call.respond(id)
			}
		}

		authenticate(Employee) {
			get {
				//TODO in #123: search criterion
				//TODO in #130: only return records that the user is allowed to see

				val results = database.records.list()

				call.respond(results)
			}
		}
	}

	/**
	 * Endpoint to manage a single record: `/api/records/{id}`.
	 *
	 * ### Get
	 *
	 * Gets detailed information on a specific record.
	 *
	 * - Requires employee authentication.
	 * - Response: [Record]
	 *
	 * ### Post `/initial`
	 *
	 * Shadows the user's initial submission.
	 * This is used to fix mistakes in the initial submission.
	 *
	 * - Requires employee authentication.
	 * - Body: [Submission]
	 * - Response: `"Success"`
	 *
	 * ### Post `/review`
	 *
	 * Make a decision about this record.
	 *
	 * - Requires employee authentication.
	 * - Body: [ApiRecordReview]
	 * - Response: `"Success"`
	 */
	fun Route.record() = route("{id}") {
		authenticate(Employee) {
			get {
				//TODO in #130: only return the record if the user is allowed to see it

				val record = call.parameters["id"]?.let { database.records.fromId(it) }
					?: error("Paramètre manquant : 'id'")

				call.respond(record.requestValue())
			}

			post("/initial") {
				//TODO in #130: refuse the edition if the user is not allowed to see it

				val record = call.parameters["id"]?.let { database.records.fromId(it) }
					?: error("Paramètre manquant : 'id'")
				val user = call.principal<Auth.AuthPrincipal>()
					?.let { database.users.fromId(it.email.email) }
					?: error("Il est obligatoire d'être connecté pour pouvoir modifier un dossier")

				val body = call.receive<Submission>()

				database.records.editInitial(record, user, body)

				call.respond("Success")
			}

			post("review") {
				//TODO in #130: refuse the review if the user is not allowed to see it

				val ref = call.parameters["id"]?.let { database.records.fromId(it) }
					?: error("Paramètre manquant : 'id'")
				val user = call.principal<Auth.AuthPrincipal>()
					?.let { database.users.fromId(it.email.email) }
					?: error("Il est obligatoire d'être connecté pour pouvoir modifier un dossier")

				val body = call.receive<ApiRecordReview>()

				val record = ref.requestValueOrThrow()
				val form = record.form.requestValueOrThrow().version(record.formVersion)
					?: error("La version ${record.formVersion} du formulaire ${record.form} est introuvable")

				val nextStep = when (val decision = body.decision) {
					is Record.Decision.Accepted -> {
						val currentStep =
							requireNotNull(record.currentStep) { "Le dossier est dans l'état refusé, il n'est pas possible de l'accepter" }
						if (form.reviewSteps.size > currentStep)
							currentStep + 1
						else
							currentStep // accepted but there is no next step, keep in the current one (same as Snoozed)
					}

					is Record.Decision.MovedToPreviousStep -> {
						require(
							decision.step < (record.currentStep ?: Int.MAX_VALUE)
						) { "Le dossier est dans l'état ${record.currentStep?.toString() ?: "refusé"}, il est impossible de le déplacer vers l'état ${decision.step} qui n'est pas un état précédent" }
						decision.step
					}

					is Record.Decision.Refused -> null // 'null' is the refused step
					is Record.Decision.Snoozed -> record.currentStep
				}

				database.records.review(ref, user, nextStep, body.decision, body.reason, body.submission)

				call.respond("Success")
			}
		}
	}
}
