package formulaide.server.routes

import formulaide.api.bones.*
import formulaide.api.dsl.form
import formulaide.core.field.FlatField
import formulaide.core.field.resolve
import formulaide.core.form.Form
import formulaide.core.form.Template
import formulaide.server.Auth.Companion.Employee
import formulaide.server.Auth.Companion.requireAdmin
import formulaide.server.database
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import opensavvy.backbone.Ref.Companion.requestValue

/**
 * The schema management endpoint: `/api/schema`.
 *
 * Schema management handles fields, templates and forms.
 */
@Suppress("MemberVisibilityCanBePrivate")
object SchemaRouting {

	internal fun Routing.enable() = route("/api/schema") {
		fields()
		templates()
		forms()
	}

	/**
	 * The field management endpoint: `/api/schema/fields`.
	 *
	 * ### Get `/{id}`
	 *
	 * Gets a specific field instance.
	 *
	 * - Response: [FlatField.Container]
	 *
	 * ### Post
	 *
	 * Creates a new field instance.
	 *
	 * - Requires administrator authentication
	 * - Body: [ApiNewFields]
	 * - Response: identifier of the created instance ([String])
	 */
	fun Route.fields() = route("/fields") {
		get("/{id}") {
			val id = call.parameters["id"] ?: error("Paramètre manquant : 'id'")

			val ref = database.fields.fromId(id)
			val field = ref.requestValue()

			call.respond(field)
		}

		authenticate(Employee) {
			post {
				call.requireAdmin(database)

				val input = call.receive<ApiNewFields>()
				val ref = database.fields.create(input.name, input.root.resolve())
				call.respond(ref)
			}
		}
	}

	/**
	 * The template management endpoint: `/api/schema/templates`.
	 * To manage a single template, see [template].
	 *
	 * ### Get
	 *
	 * Gets the list of all templates.
	 *
	 * - Response: list of template identifiers ([String])
	 *
	 * ### Post
	 *
	 * Creates a new template.
	 *
	 * - Body: [ApiNewTemplate]
	 * - Response: identifier of the created template ([String])
	 */
	fun Route.templates() = route("/templates") {
		template()

		get {
			call.respond(database.templates.all())
		}

		authenticate(Employee) {
			post {
				call.requireAdmin(database)

				val template = call.receive<ApiNewTemplate>()
				val ref = database.templates.create(template.name, template.firstVersion)
				call.respond(ref)
			}
		}
	}

	/**
	 * The single template management endpoint: `/api/schema/templates/{id}`.
	 * To manage multiple templates, see [templates].
	 *
	 * ### Get
	 *
	 * Requests a single template.
	 *
	 * - Body: [Template]
	 *
	 * ### Post `/version`
	 *
	 * Adds a new version to this template.
	 *
	 * - Requires administrator authentication
	 * - Body: [Template.Version]
	 * - Response: identifier of the modified template ([String])
	 *
	 * ### Patch
	 *
	 * Edits this template.
	 *
	 * - Requires administrator authentication
	 * - Body: [ApiTemplateEdition]
	 * - Response: identifier of the modified template ([String])
	 */
	fun Route.template() = route("/{id}") {
		get {
			val id = call.parameters["id"] ?: error("Le paramètre 'id' est manquant")
			val template = database.templates.fromId(id)
			call.respond(template.requestValue())
		}

		authenticate(Employee) {
			post("/version") {
				call.requireAdmin(database)

				val id = call.parameters["id"] ?: error("Le paramètre 'id' est manquant")
				val ref = database.templates.fromId(id)
				val newVersion = call.receive<Template.Version>()

				database.templates.createVersion(ref, newVersion)

				call.respond(ref)
			}

			patch {
				call.requireAdmin(database)

				val id = call.parameters["id"] ?: error("Le paramètre 'id' est manquant")
				val ref = database.templates.fromId(id)
				val edits = call.receive<ApiTemplateEdition>()

				database.templates.edit(ref, edits.name)

				call.respond(ref)
			}
		}
	}

	/**
	 * The form management endpoint: `/api/schema/forms`.
	 * To manage a single form, see [form].
	 *
	 * ### Get
	 *
	 * Gets the list of forms.
	 *
	 * - By default, closed forms are not included.
	 * - The optional parameter `includeClosed` is set to `true` to include closed forms (requires administrator authentication)
	 * - Response: list of form identifiers ([String])
	 *
	 * ### Post
	 *
	 * Creates a new form.
	 *
	 * - Requires administrator authentication
	 * - Body: [ApiNewForm]
	 * - Response: identifier of the created form ([String])
	 */
	fun Route.forms() = route("/forms") {
		form()

		authenticate(Employee, optional = true) {
			get {
				val includeClosed = call.parameters["includeClosed"].toBoolean()

				if (includeClosed)
					call.requireAdmin(database)

				val results = database.forms.all(includeClosed)
				call.respond(results)
			}
		}

		authenticate(Employee) {
			post {
				call.requireAdmin(database)

				val body = call.receive<ApiNewForm>()
				val ref = database.forms.create(body.name, body.firstVersion, body.public)

				call.respond(ref)
			}
		}
	}

	/**
	 * The single form management endpoint: `/api/schema/forms/{id}`.
	 * To manage multiple forms, see [forms].
	 *
	 * ### Get
	 *
	 * Gets information about a form.
	 *
	 * - Response: [Form]
	 *
	 * ### Post `/version`
	 *
	 * Adds a new version to this form.
	 *
	 * - Requires administrator authentication
	 * - Body: [Form.Version]
	 * - Response: identifier of the modified form ([String])
	 *
	 * ### Patch
	 *
	 * Edits a form.
	 *
	 * - Requires administrator authentication
	 * - Body: [ApiFormEdition]
	 * - Response: identifier of the modified form ([String])
	 */
	fun Route.form() = route("/{id}") {
		get {
			val id = call.parameters["id"] ?: error("Paramètre manquant : 'id'")

			val ref = database.forms.fromId(id)
			val form = ref.requestValue()

			call.respond(form)
		}

		authenticate(Employee) {
			post("/version") {
				call.requireAdmin(database)

				val id = call.parameters["id"] ?: error("Paramètre manquant : 'id'")
				val ref = database.forms.fromId(id)
				val body = call.receive<Form.Version>()
				database.forms.createVersion(ref, body)

				call.respond(ref)
			}

			patch {
				call.requireAdmin(database)

				val id = call.parameters["id"] ?: error("Paramètre manquant : 'id'")
				val ref = database.forms.fromId(id)
				val body = call.receive<ApiFormEdition>()
				database.forms.edit(ref, body.name, body.public, body.open)

				call.respond(ref)
			}
		}
	}
}
