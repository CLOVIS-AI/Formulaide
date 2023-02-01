package opensavvy.formulaide.remote.server

import io.ktor.server.routing.*
import opensavvy.backbone.Ref.Companion.request
import opensavvy.formulaide.core.Department
import opensavvy.formulaide.remote.api
import opensavvy.formulaide.remote.dto.DepartmentDto.Companion.toDto
import opensavvy.spine.Identified
import opensavvy.spine.ktor.server.route
import opensavvy.state.progressive.firstValue

fun Routing.departments(departments: Department.Service) {
	route(api.departments.get, contextGenerator) {
		departments.list(includeClosed = parameters.includeClosed)
			.bind()
			.map { api.departments.id.idOf(it.id) }
	}

	route(api.departments.id.get, contextGenerator) {
		api.departments.id.refOf(id, departments).bind()
			.request().firstValue().bind()
			.toDto()
	}

	route(api.departments.create, contextGenerator) {
		departments.create(body.name).bind()
			.let { api.departments.id.idOf(it.id) }
			.let { Identified(it, Unit) }
	}

	route(api.departments.id.edit, contextGenerator) {
		val ref = api.departments.id.refOf(id, departments).bind()

		departments.edit(ref, open = body.open).bind()
	}
}
