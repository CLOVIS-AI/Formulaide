package opensavvy.formulaide.server

import io.ktor.server.auth.*
import io.ktor.server.routing.*
import opensavvy.backbone.Ref.Companion.request
import opensavvy.formulaide.core.Department
import opensavvy.formulaide.remote.api
import opensavvy.formulaide.remote.dto.DepartmentDto.Companion.toDto
import opensavvy.formulaide.server.utils.authenticated
import opensavvy.spine.Identified
import opensavvy.spine.ktor.server.route
import opensavvy.state.firstValue

fun Routing.departments(departments: Department.Service) = authenticate(optional = true) {
	route(api.departments.get, contextGenerator) {
		authenticated {
			departments.list(includeClosed = parameters.includeClosed)
				.bind()
				.map { api.departments.id.idOf(it.id) }
		}
	}

	route(api.departments.id.get, contextGenerator) {
		authenticated {
			api.departments.id.refOf(id, departments).bind()
				.request().firstValue().bind()
				.toDto()
		}
	}

	route(api.departments.create, contextGenerator) {
		authenticated {
			departments.create(body.name).bind()
				.let { api.departments.id.idOf(it.id) }
				.let { Identified(it, Unit) }
		}
	}

	route(api.departments.id.edit, contextGenerator) {
		authenticated {
			val ref = api.departments.id.refOf(id, departments).bind()

			departments.edit(ref, open = body.open).bind()
		}
	}
}
