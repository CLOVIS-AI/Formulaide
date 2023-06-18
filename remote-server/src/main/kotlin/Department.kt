package opensavvy.formulaide.remote.server

import io.ktor.server.routing.*
import opensavvy.formulaide.core.Department
import opensavvy.formulaide.remote.api
import opensavvy.formulaide.remote.dto.DepartmentDto.Companion.toDto
import opensavvy.formulaide.remote.server.utils.notFound
import opensavvy.formulaide.remote.server.utils.unauthenticated
import opensavvy.formulaide.remote.server.utils.unauthorized
import opensavvy.spine.Identified
import opensavvy.spine.ktor.server.route
import opensavvy.state.arrow.toEither
import opensavvy.state.coroutines.now
import opensavvy.state.outcome.mapFailure

fun Routing.departments(departments: Department.Service<*>) {
	route(api.departments.get, contextGenerator) {
		departments.list(includeClosed = parameters.includeClosed)
			.mapFailure {
				when (it) {
					Department.Failures.Unauthenticated -> unauthenticated()
					Department.Failures.Unauthorized -> unauthorized()
				}
			}
			.toEither()
			.bind()
			.map { api.departments.id.idOf(it.toIdentifier().text) }
	}

	route(api.departments.id.get, contextGenerator) {
		departments.fromIdentifier(api.departments.id.identifierOf(id))
			.request().now()
			.mapFailure {
				when (it) {
					is Department.Failures.NotFound -> notFound(it.id)
					Department.Failures.Unauthenticated -> unauthenticated()
				}
			}
			.toEither()
			.bind()
			.toDto()
	}

	route(api.departments.create, contextGenerator) {
		departments.create(body.name)
			.mapFailure {
				when (it) {
					Department.Failures.Unauthenticated -> unauthenticated()
					Department.Failures.Unauthorized -> unauthorized()
				}
			}
			.toEither()
			.bind()
			.let { api.departments.id.idOf(it.toIdentifier().text) }
			.let { Identified(it, Unit) }
	}

	route(api.departments.id.edit, contextGenerator) {
		val ref = departments.fromIdentifier(api.departments.id.identifierOf(id))

		ref.edit(open = body.open)
			.mapFailure {
				when (it) {
					is Department.Failures.NotFound -> notFound(ref)
					Department.Failures.Unauthenticated -> unauthenticated()
					Department.Failures.Unauthorized -> unauthorized()
				}
			}
			.toEither()
			.bind()
	}
}
