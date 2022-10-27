package opensavvy.formulaide.api.server

import io.ktor.server.routing.*
import kotlinx.coroutines.flow.emitAll
import kotlinx.datetime.toInstant
import opensavvy.backbone.Backbone.Companion.request
import opensavvy.formulaide.api.Context
import opensavvy.formulaide.api.api2
import opensavvy.formulaide.api.toApi
import opensavvy.formulaide.api.toCore
import opensavvy.formulaide.core.Template
import opensavvy.formulaide.database.Database
import opensavvy.formulaide.state.bind
import opensavvy.formulaide.state.mapSuccess
import opensavvy.spine.ktor.server.ContextGenerator
import opensavvy.spine.ktor.server.route
import opensavvy.formulaide.api.Template as ApiTemplate

fun Routing.templates(database: Database, contextGenerator: ContextGenerator<Context>) {

	route(api2.templates.get, contextGenerator) {
		emitAll(
			database.templates.list(parameters.includeClosed)
				.mapSuccess { list ->
					list.map { api2.templates.id.idOf(it.id) }
				}
		)
	}

	route(api2.templates.create, contextGenerator) {
		val result = database.templates.create(
			body.name, Template.Version(
				body.firstVersion.creationDate,
				body.firstVersion.title,
				body.firstVersion.field.toCore(database.templates, database.templates.versions),
			)
		).mapSuccess { api2.templates.id.idOf(it.id) to Unit }

		emitAll(result)
	}

	route(api2.templates.id.create, contextGenerator) {
		val templateId = bind(api2.templates.id.idFrom(id, context))

		val result = database.templates.createVersion(
			Template.Ref(templateId, database.templates),
			Template.Version(
				body.creationDate,
				body.title,
				body.field.toCore(database.templates, database.templates.versions),
			)
		).mapSuccess { api2.templates.id.version.idOf(it.template.id, it.version.toString()) to Unit }

		emitAll(result)
	}

	route(api2.templates.id.edit, contextGenerator) {
		val templateId = bind(api2.templates.id.idFrom(id, context))

		val result = database.templates.edit(
			Template.Ref(templateId, database.templates),
			body.name,
			body.open,
		)

		emitAll(result)
	}

	route(api2.templates.id.get, contextGenerator) {
		val templateId = bind(api2.templates.id.idFrom(id, context))

		val result = database.templates.request(Template.Ref(templateId, database.templates))
			.mapSuccess { template ->
				ApiTemplate(
					template.name,
					template.open,
					template.versions.map { api2.templates.id.version.idOf(it.template.id, it.version.toString()) }
				)
			}

		emitAll(result)
	}

	route(api2.templates.id.version.get, contextGenerator) {
		val (templateId, versionId) = bind(api2.templates.id.version.idFrom(id, context))

		val result = database.templates.versions.request(
			Template.Version.Ref(
				Template.Ref(templateId, database.templates),
				versionId.toInstant(),
				database.templates.versions,
			)
		).mapSuccess { version ->
			ApiTemplate.Version(
				version.creationDate,
				version.title,
				version.field.toApi()
			)
		}

		emitAll(result)
	}
}
