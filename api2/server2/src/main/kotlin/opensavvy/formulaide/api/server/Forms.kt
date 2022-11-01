package opensavvy.formulaide.api.server

import io.ktor.server.routing.*
import kotlinx.coroutines.flow.first
import kotlinx.datetime.toInstant
import opensavvy.backbone.Backbone.Companion.request
import opensavvy.formulaide.api.Context
import opensavvy.formulaide.api.api2
import opensavvy.formulaide.api.toApi
import opensavvy.formulaide.api.toCore
import opensavvy.formulaide.core.Department
import opensavvy.formulaide.core.Form
import opensavvy.formulaide.database.Database
import opensavvy.spine.ktor.server.ContextGenerator
import opensavvy.spine.ktor.server.route
import opensavvy.formulaide.api.Form as ApiForm

fun Routing.forms(database: Database, contextGenerator: ContextGenerator<Context>) {

	route(api2.forms.get, contextGenerator) {
		database.forms.list(parameters.includeClosed, parameters.includePrivate).bind()
			.map { api2.templates.id.idOf(it.id) }
	}

	route(api2.forms.create, contextGenerator) {
		ensureAdministrator { "Seuls les administrateurs peuvent créer des formulaires" }

		val result = database.forms.create(
			body.name,
			body.public,
			Form.Version(
				body.firstVersion.creationDate,
				body.firstVersion.title,
				body.firstVersion.field.toCore(database.templates, database.templates.versions),
				body.firstVersion.steps.map {
					Form.Step(
						it.id,
						Department.Ref(api2.departments.id.idFrom(it.department, context).bind(), database.departments),
						it.field?.toCore(database.templates, database.templates.versions),
					)
				}
			)
		).bind()

		api2.forms.id.idOf(result.id) to Unit
	}

	route(api2.forms.id.create, contextGenerator) {
		ensureAdministrator { "Seuls les administrateurs peuvent créer des versions de formulaires" }

		val formId = api2.forms.id.idFrom(id, context).bind()

		val result = database.forms.createVersion(
			Form.Ref(formId, database.forms),
			Form.Version(
				body.creationDate,
				body.title,
				body.field.toCore(database.templates, database.templates.versions),
				body.steps.map {
					Form.Step(
						it.id,
						Department.Ref(api2.departments.id.idFrom(it.department, context).bind(), database.departments),
						it.field?.toCore(database.templates, database.templates.versions),
					)
				}
			)
		).bind()

		api2.forms.id.version.idOf(result.form.id, result.version.toString()) to Unit
	}

	route(api2.forms.id.edit, contextGenerator) {
		ensureAdministrator { "Seuls les administrateurs peuvent modifier des formulaires" }

		val formId = api2.forms.id.idFrom(id, context).bind()

		database.forms.edit(
			Form.Ref(formId, database.forms),
			body.name,
			body.public,
			body.open,
		).bind()
	}

	route(api2.forms.id.get, contextGenerator) {
		val formId = api2.forms.id.idFrom(id, context).bind()

		val form = database.forms.request(Form.Ref(formId, database.forms)).first().bind()
		ApiForm(
			form.name,
			form.public,
			form.open,
			form.versions.map { api2.forms.id.version.idOf(it.form.id, it.version.toString()) }
		)
	}

	route(api2.forms.id.version.get, contextGenerator) {
		val (formId, versionId) = api2.forms.id.version.idFrom(id, context).bind()

		val version = database.forms.versions.request(
			Form.Version.Ref(
				Form.Ref(formId, database.forms),
				versionId.toInstant(),
				database.forms.versions,
			)
		).first().bind()

		ApiForm.Version(
			version.creationDate,
			version.title,
			version.field.toApi(),
			version.reviewSteps.map { step ->
				ApiForm.Step(
					step.id,
					api2.departments.id.idOf(step.reviewer.id),
					step.field?.toApi()
				)
			}
		)
	}
}
