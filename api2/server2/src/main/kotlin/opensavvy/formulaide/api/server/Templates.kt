package opensavvy.formulaide.api.server

import io.ktor.server.routing.*
import kotlinx.coroutines.flow.first
import kotlinx.datetime.toInstant
import opensavvy.backbone.Backbone.Companion.request
import opensavvy.formulaide.api.Context
import opensavvy.formulaide.api.api2
import opensavvy.formulaide.api.toApi
import opensavvy.formulaide.api.toCore
import opensavvy.formulaide.core.Template
import opensavvy.formulaide.database.Database
import opensavvy.spine.ktor.server.ContextGenerator
import opensavvy.spine.ktor.server.route
import opensavvy.formulaide.api.Template as ApiTemplate

fun Routing.templates(database: Database, contextGenerator: ContextGenerator<Context>) {

	route(api2.templates.get, contextGenerator) {
		database.templates.list(parameters.includeClosed).bind()
			.map { api2.templates.id.idOf(it.id) }
	}

	route(api2.templates.create, contextGenerator) {
		ensureAdministrator { "Seuls les administrateurs peuvent créer des modèles" }

		val result = database.templates.create(
			body.name, Template.Version(
				body.firstVersion.creationDate,
				body.firstVersion.title,
				body.firstVersion.field.toCore(database.templates, database.templates.versions),
			)
		).bind()

		api2.templates.id.idOf(result.id) to Unit
	}

	route(api2.templates.id.create, contextGenerator) {
		ensureAdministrator { "Seuls les administrateurs peuvent créer des versions de modèles" }

		val templateId = api2.templates.id.idFrom(id, context).bind()

		val result = database.templates.createVersion(
			Template.Ref(templateId, database.templates),
			Template.Version(
				body.creationDate,
				body.title,
				body.field.toCore(database.templates, database.templates.versions),
			)
		).bind()

		api2.templates.id.version.idOf(result.template.id, result.version.toString()) to Unit
	}

	route(api2.templates.id.edit, contextGenerator) {
		ensureAdministrator { "Seuls les administrateurs peuvent modifier des modèles" }

		val templateId = api2.templates.id.idFrom(id, context).bind()

		database.templates.edit(
			Template.Ref(templateId, database.templates),
			body.name,
			body.open,
		)
	}

	route(api2.templates.id.get, contextGenerator) {
		val templateId = api2.templates.id.idFrom(id, context).bind()

		val template = database.templates.request(Template.Ref(templateId, database.templates)).first().bind()

		ApiTemplate(
			template.name,
			template.open,
			template.versions.map { api2.templates.id.version.idOf(it.template.id, it.version.toString()) }
		)
	}

	route(api2.templates.id.version.get, contextGenerator) {
		val (templateId, versionId) = api2.templates.id.version.idFrom(id, context).bind()

		val version = database.templates.versions.request(
			Template.Version.Ref(
				Template.Ref(templateId, database.templates),
				versionId.toInstant(),
				database.templates.versions,
			)
		).first().bind()

		ApiTemplate.Version(
			version.creationDate,
			version.title,
			version.field.toApi()
		)
	}
}
