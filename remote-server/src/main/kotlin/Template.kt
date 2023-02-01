package opensavvy.formulaide.remote.server

import io.ktor.server.routing.*
import opensavvy.backbone.Ref.Companion.request
import opensavvy.formulaide.core.Template
import opensavvy.formulaide.remote.api
import opensavvy.formulaide.remote.dto.SchemaDto.Companion.toDto
import opensavvy.formulaide.remote.dto.SchemaDto.Companion.toTemplateVersion
import opensavvy.spine.Identified
import opensavvy.spine.ktor.server.route
import opensavvy.state.progressive.firstValue

fun Routing.templates(
	templates: Template.Service,
) {

	route(api.templates.get, contextGenerator) {
		templates.list(includeClosed = parameters.includeClosed)
			.bind()
			.map { api.templates.id.idOf(it.id) }
	}

	route(api.templates.create, contextGenerator) {
		templates.create(
			body.name,
			body.firstVersion.toTemplateVersion(decodeTemplate = {
				api.templates.id.version.refOf(it, templates).bind()
			})
		).bind()
			.let { api.templates.id.idOf(it.id) }
			.let { Identified(it, Unit) }
	}

	route(api.templates.id.create, contextGenerator) {
		val ref = api.templates.id.refOf(id, templates).bind()

		templates.createVersion(
			ref,
			body.toTemplateVersion(
				decodeTemplate = {
					api.templates.id.version.refOf(it, templates).bind()
				}
			)
		).bind()
			.let { api.templates.id.version.idOf(it.template.id, it.version.toString()) }
			.let { Identified(it, Unit) }
	}

	route(api.templates.id.edit, contextGenerator) {
		val ref = api.templates.id.refOf(id, templates).bind()

		templates.edit(
			ref,
			name = body.name,
			open = body.open,
		).bind()
	}

	route(api.templates.id.get, contextGenerator) {
		val ref = api.templates.id.refOf(id, templates).bind()

		ref.request().firstValue().bind()
			.toDto()
	}

	route(api.templates.id.version.get, contextGenerator) {
		val version = api.templates.id.version.refOf(id, templates).bind()

		version.request().firstValue().bind()
			.toDto()
	}

}
