package formulaide.server.routes

import formulaide.api.data.Form
import formulaide.api.data.FormMetadata
import formulaide.api.fields.FormField
import formulaide.api.fields.SimpleField
import formulaide.api.types.Arity
import formulaide.api.types.ReferenceId
import formulaide.db.document.*
import formulaide.server.Auth.Companion.Employee
import formulaide.server.Auth.Companion.requireAdmin
import formulaide.server.Auth.Companion.requireEmployee
import formulaide.server.database
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.html.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.html.*

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

			post("/editMetadata") {
				call.requireAdmin(database)

				val metadata = call.receive<FormMetadata>()
				val form = database.editForm(metadata)

				call.respond(form)
			}
		}

		get("/html") {
			val formId = call.parameters["id"]
				?: error("Le paramètre GET 'id' est obligatoire : ${call.parameters.entries()}")
			val apiUrl = call.parameters["url"]
				?: error("Le paramètre GET 'url' est obligatoire : ${call.parameters.entries()}")
			val form = database.findForm(formId)?.takeIf { it.public }
				?: error("Le formulaire demandé n'existe pas, ou n'est pas public : $formId")
			require(form.open) { "Le formulaire demandé a été archivé, il n'est plus possible d'y répondre." }

			val composites = database.listComposites()
			form.load(composites)

			call.respondHtml {
				head {
					meta(charset = Charsets.UTF_8.name())
					title(form.name)
				}

				body {
					form(
						action = "$apiUrl/submissions/nativeCreate/${form.id}",
						method = FormMethod.post,
						encType = FormEncType.multipartFormData,
					) {

						h1(formTitleClass) { form.name }

						for (field in form.mainFields.fields) {
							generateFieldHtml(field, field.id)
						}

						input(InputType.submit) { value = "Envoyer" }
						input(InputType.reset) { value = "Effacer" }
					}
				}
			}
		}
	}
}

private const val fieldNameClass = "field-name"
private const val formTitleClass = "form-title"
private const val requiredClass = "required-mark"
private const val allowedFormatsClass = "formats"
private const val compositeClass = "composite"
private const val unionClass = "union"

private fun FlowContent.generateFieldHtml(
	field: FormField,
	fieldId: String,
	overrideArity: Arity? = null,
): Unit = when {
	overrideArity == null && field.arity.max > 1 -> {
		repeat(field.arity.min) {
			generateFieldHtml(field,
			                  "$fieldId:$it",
			                  overrideArity = Arity.mandatory())
		}
		repeat(field.arity.max - field.arity.min) {
			generateFieldHtml(field,
			                  "$fieldId:${it + field.arity.min}",
			                  overrideArity = Arity.optional())
		}
	}
	field is FormField.Simple -> {
		if (field.simple is SimpleField.Message) {
			p(fieldNameClass) { +field.name }
		} else {
			label(fieldNameClass) {
				htmlFor = fieldId
				+field.name
			}

			val arity = overrideArity ?: field.arity
			val mandatory = arity.min >= 1

			if (mandatory)
				label(requiredClass) {
					htmlFor = fieldId
					+"*"
				}

			when (val simple = field.simple) {
				is SimpleField.Text -> input(InputType.text, name = fieldId) {
					id = fieldId
					required = mandatory
					maxLength = simple.effectiveMaxLength.toString()
				}
				is SimpleField.Email -> input(InputType.email, name = fieldId) {
					id = fieldId
					required = mandatory
				}
				is SimpleField.Phone -> input(InputType.tel, name = fieldId) {
					id = fieldId
					required = mandatory
				}
				is SimpleField.Boolean -> {
					input(InputType.hidden, name = fieldId) { value = "false" }
					input(InputType.checkBox, name = fieldId) { value = "true"; id = fieldId }
				}
				is SimpleField.Integer -> input(InputType.number, name = fieldId) {
					id = fieldId
					required = mandatory
					min = simple.effectiveMin.toString()
					max = simple.effectiveMax.toString()
					step = "1"
				}
				is SimpleField.Decimal -> input(InputType.number, name = fieldId) {
					id = fieldId
					required = mandatory
				}
				is SimpleField.Date -> input(InputType.date, name = fieldId) {
					id = fieldId
					required = mandatory
				}
				is SimpleField.Time -> input(InputType.time, name = fieldId) {
					id = fieldId
					required = mandatory
				}
				is SimpleField.Upload -> {
					label(allowedFormatsClass) {
						htmlFor = fieldId
						+"Formats autorisés : ${
							simple.allowedFormats
								.flatMap { it.extensions }
								.joinToString(separator = ", ")
						}"
					}
					input(InputType.file, name = fieldId) {
						id = fieldId
						required = mandatory
						multiple = false
						accept = simple.allowedFormats.flatMap { it.mimeTypes }
							.joinToString(separator = ",")
					}
				}
				SimpleField.Message -> error("Un champ de type 'Message' aurait du être généré précédemment : $simple")
			}
		}
	}
	field is FormField.Union<*> -> {
		label(fieldNameClass) {
			+field.name
		}

		val arity = overrideArity ?: field.arity
		val mandatory = arity.min >= 1

		if (mandatory)
			label(requiredClass) {
				+"*"
			}

		for (subField in field.options) {
			input(InputType.radio, name = fieldId) {
				id = "$fieldId:${subField.id}+"
				required = mandatory
				value = subField.id

				val self = "'$fieldId:${subField.id}-'"
				val others = field.options
					.filter { it != subField }
					.joinToString(
						separator = ",",
						prefix = "[",
						postfix = "]"
					) { "'$fieldId:${it.id}-'" }

				//language=JavaScript
				onChange = """
					const me = document.getElementById($self)
					me.disabled = false
                    me.hidden = false
                    $others.forEach(id => {
                  		const o = document.getElementById(id)
						o.disabled = true
						o.hidden = true
					})
				""".trimIndent()
					.replace("\n", "")
			}
			label {
				htmlFor = "$fieldId:${subField.id}+"
				+subField.name
			}
		}

		for (subField in field.options) {
			fieldSet(unionClass) {
				id = "$fieldId:${subField.id}-"
				legend { +field.name }

				generateFieldHtml(subField, "$fieldId:${subField.id}")
			}
		}
	}
	field is FormField.Composite -> {
		fieldSet(compositeClass) {
			id = fieldId
			legend {
				+field.name
			}

			for (subField in field.fields)
				generateFieldHtml(subField, "$fieldId:${subField.id}")
		}
	}
	else -> error("IMPOSSIBLE: le champ ne correspond à aucun type autorisé : $field")
}
