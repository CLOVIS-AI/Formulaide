package formulaide.server.routes

import formulaide.api.data.*
import formulaide.api.fields.FormField
import formulaide.api.fields.FormRoot
import formulaide.api.fields.asSequence
import formulaide.api.fields.asSequenceWithKey
import formulaide.api.types.Ref
import formulaide.api.types.Ref.Companion.createRef
import formulaide.api.types.Ref.Companion.load
import formulaide.api.types.UploadRequest
import formulaide.api.users.canAccess
import formulaide.db.document.*
import formulaide.server.Auth
import formulaide.server.Auth.Companion.requireEmployee
import formulaide.server.database
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

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
				val name = it.name
					?: error("La partie '${it.contentDisposition}' de type '${it.contentType}' devrait avoir un nom.")

				when (it) {
					is PartData.FormItem -> data[name] = it.value
					is PartData.FileItem -> {
						val fieldIds = name.split(":")
						var fieldKey = fieldIds.getOrNull(0)
							?: error("L'identifiant '$name' devrait contenir au moins une sous-partie.")
						var field: FormField = form.mainFields.fields.find { it.id == fieldKey }
							?: error("Le champ $fieldKey n'existe pas.")
						val fieldIdsIterator = fieldIds.drop(1).iterator()
						while (fieldIdsIterator.hasNext()) {
							if (field.arity.max > 1) {
								// If the field is a list, then we can ignore the next ID (which is an index in that list)
								fieldIdsIterator.next()
							}

							val next = fieldIdsIterator.next()

							val nextField = when (field) {
								is FormField.Simple -> error("Un champ simple ne contient pas de sous-champs, mais le champ $next est recherché dans le champ simple $field")
								is FormField.Union<*> -> field.options.find { it.id == next }
									?: error("L'union ${field.id} ne contient pas d'option $next")
								is FormField.Composite -> field.fields.find { it.id == next }
									?: error("La donnée composiée ${field.id} ne contient pas de champ $next")
							}

							field = nextField
							fieldKey += ":" + field.id
						}

						val uploaded = uploadFile(
							UploadRequest(form.createRef(), null, fieldKey),
							it
						)
						if (uploaded != null)
							data[name] = uploaded.id
					}
					else -> error("Le type de données '${it::class}' n'est pas supporté.")
				}

				it.dispose()
			}

			val submission = FormSubmission(
				id = Ref.SPECIAL_TOKEN_NEW,
				form = form.createRef(),
				root = null, // The raw HTML can only be used for the original submission
				data = data.mapValues { (_, v) -> v.trim() },
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
				call.respond(
					database.findSubmissionById(body)?.toApi()
						?: error("La saisie '$body' est introuvable")
				)
			}

			get("/formsToReview") {
				val user = call.requireEmployee(database)
				val forms = database.findFormsAssignedTo(user)
				call.respond(forms)
			}

			post("/recordsToReview") {
				val user = call.requireEmployee(database)
				val request = call.receive<RecordsToReviewRequest>()
				val form = database.findForm(request.form.id)
					?: error("Le formulaire est introuvable : ${request.form.id}")

				require(user.toApi().canAccess(form, request.state)) {
					"Vous n'avez pas accès aux saisies du formulaire « ${form.name} » (${form.id}, utilisateur ${user.email})"
				}

				val records = submissionsMatchingRecord(request, form)

				call.respond(records)
			}

			post("/csv") {
				val request = call.receive<RecordsToReviewRequest>()
				val form = database.findForm(request.form.id)
					?: error("Le formulaire est introuvable : ${request.form.id}")
				form.load(database.listComposites())

				val output = StringBuilder()
				output.csvBuildColumns(form)

				val records = submissionsMatchingRecord(request, form, limit = null)

				for (record in records)
					output.csvBuildRow(form, record)

				call.respondText(output.toString(), ContentType.Text.CSV)
			}
		}
	}
}

private suspend fun submissionsMatchingRecord(
	request: RecordsToReviewRequest,
	form: Form,
	limit: Int? = Record.MAXIMUM_NUMBER_OF_RECORDS_PER_ACTION,
): List<Record> {
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

	return database.findRecords(form, request.state, submissions, limit)
}

private fun String.sanitizeForCsv() = this
	.replace("\n", "\\n")
	.replace(",", " ")

private fun Form.csvFields() = mainFields.asSequence(checkArity = true) +
		actions.map { it.fields ?: FormRoot(emptyList()) }
			.flatMap { it.asSequence(checkArity = true) }

private fun Form.csvFieldsWithKey() =
	mainFields.asSequenceWithKey(checkArity = true).map { "_:${it.first}" to it.second } +
			actions.map { it.id to (it.fields ?: FormRoot(emptyList())) }
				.flatMap { (rootId, root) ->
					root.asSequenceWithKey(checkArity = true).map { "$rootId:${it.first}" to it.second }
				}

private fun StringBuilder.csvBuildColumns(form: Form) {
	// Column ID
	for ((key, _) in form.csvFieldsWithKey()) {
		append(key)
		append(',')
	}
	append('\n')

	// Column name
	for (field in form.csvFields()) {
		append(field.name.sanitizeForCsv())
		append(',')
	}
	append('\n')
}

private suspend fun StringBuilder.csvBuildRow(form: Form, record: Record) {
	fun csvBuildField(field: FormField, submission: FormSubmission, key: String) {
		repeat(field.arity.max) {
			val currentKey =
				if (field.arity.max <= 1) key
				else "$key:$it"

			append(submission.data[currentKey]?.sanitizeForCsv() ?: "")
			append(',')

			if (field is FormField.Union<*>)
				for (subField in field.options)
					csvBuildField(subField, submission, "$currentKey:${subField.id}")

			if (field is FormField.Composite)
				for (subField in field.fields)
					csvBuildField(subField, submission, "$currentKey:${subField.id}")
		}
	}

	val initialSubmission = record.history
		.asSequence()
		.filter { it.previousState == null }
		.maxByOrNull { it.timestamp }
		?.fields
		?: error("Ce dossier n'a pas de première étape : ${record.id}")
	initialSubmission.load {
		database.findSubmissionById(it)?.toApi() ?: error("La saisie est introuvable : $it")
	}
	for (field in form.mainFields.fields) {
		csvBuildField(field, initialSubmission.obj, field.id)
	}

	for (action in form.actions) {
		val submission = record.history
			.asSequence()
			.filter { it.previousState == RecordState.Action(action.createRef()) }
			.maxByOrNull { it.timestamp }
			?.fields
			?: continue
		submission.load {
			database.findSubmissionById(it)?.toApi() ?: error("La saisie est introuvable : $it")
		}

		for (field in action.fields?.fields ?: emptyList())
			csvBuildField(field, submission.obj, field.id)
	}

	append('\n')
}
