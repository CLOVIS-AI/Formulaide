package opensavvy.formulaide.remote.server

import io.ktor.server.routing.*
import opensavvy.backbone.Ref.Companion.request
import opensavvy.formulaide.core.Department
import opensavvy.formulaide.core.Form
import opensavvy.formulaide.core.Template
import opensavvy.formulaide.remote.api
import opensavvy.formulaide.remote.dto.SchemaDto.Companion.toDto
import opensavvy.formulaide.remote.dto.SchemaDto.Companion.toForm
import opensavvy.spine.Identified
import opensavvy.spine.ktor.server.route
import opensavvy.state.progressive.firstValue

fun Routing.forms(
	departments: Department.Service,
	templates: Template.Service,
	forms: Form.Service,
) {

	route(api.forms.get, contextGenerator) {
		forms.list(includeClosed = parameters.includeClosed)
			.bind()
			.map { api.templates.id.idOf(it.id) }
	}

	route(api.forms.create, contextGenerator) {
		forms.create(
			body.name,
			body.firstVersion.toForm(
				decodeDepartment = {
					api.departments.id.refOf(it, departments).bind()
				},
				decodeTemplate = {
					api.templates.id.version.refOf(it, templates).bind()
				}
			)
		).bind()
			.let { api.templates.id.idOf(it.id) }
			.let { Identified(it, Unit) }
	}

	route(api.forms.id.create, contextGenerator) {
		val ref = api.forms.id.refOf(id, forms).bind()

		forms.createVersion(
			ref,
			body.toForm(
				decodeDepartment = {
					api.departments.id.refOf(it, departments).bind()
				},
				decodeTemplate = {
					api.templates.id.version.refOf(it, templates).bind()
				}
			)
		).bind()
			.let { api.forms.id.version.idOf(it.form.id, it.version.toString()) }
			.let { Identified(it, Unit) }
	}

	route(api.forms.id.edit, contextGenerator) {
		val ref = api.forms.id.refOf(id, forms).bind()

		forms.edit(
			ref,
			name = body.name,
			open = body.open,
			public = body.public,
		).bind()
	}

	route(api.forms.id.get, contextGenerator) {
		val ref = api.forms.id.refOf(id, forms).bind()

		ref.request().firstValue().bind()
			.toDto()
	}

	route(api.forms.id.version.get, contextGenerator) {
		val version = api.forms.id.version.refOf(id, forms).bind()

		version.request().firstValue().bind()
			.toDto()
	}

}
