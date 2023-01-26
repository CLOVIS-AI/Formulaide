package opensavvy.formulaide.remote.server

import io.ktor.server.auth.*
import io.ktor.server.routing.*
import opensavvy.backbone.Ref.Companion.request
import opensavvy.formulaide.core.Template
import opensavvy.formulaide.remote.api
import opensavvy.formulaide.remote.dto.SchemaDto.Companion.toDto
import opensavvy.formulaide.remote.dto.SchemaDto.Companion.toTemplateVersion
import opensavvy.formulaide.remote.server.utils.authenticated
import opensavvy.spine.Identified
import opensavvy.spine.ktor.server.route
import opensavvy.state.progressive.firstValue

fun Routing.templates(
	templates: Template.Service,
) = authenticate(optional = true) {

	route(api.templates.get, contextGenerator) {
		authenticated {
			templates.list(includeClosed = parameters.includeClosed)
				.bind()
				.map { api.templates.id.idOf(it.id) }
		}
	}

	route(api.templates.create, contextGenerator) {
		authenticated {
			templates.create(
				body.name,
				body.firstVersion.toTemplateVersion(decodeTemplate = {
					api.templates.id.version.refOf(it, templates).bind()
				})
			).bind()
				.let { api.templates.id.idOf(it.id) }
				.let { Identified(it, Unit) }
		}
	}

	route(api.templates.id.create, contextGenerator) {
		authenticated {
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
	}

	route(api.templates.id.edit, contextGenerator) {
		authenticated {
			val ref = api.templates.id.refOf(id, templates).bind()

			templates.edit(
				ref,
				name = body.name,
				open = body.open,
			).bind()
		}
	}

	route(api.templates.id.get, contextGenerator) {
		authenticated {
			val ref = api.templates.id.refOf(id, templates).bind()

			ref.request().firstValue().bind()
				.toDto()
		}
	}

	route(api.templates.id.version.get, contextGenerator) {
		authenticated {
			val version = api.templates.id.version.refOf(id, templates).bind()

			version.request().firstValue().bind()
				.toDto()
		}
	}

}
