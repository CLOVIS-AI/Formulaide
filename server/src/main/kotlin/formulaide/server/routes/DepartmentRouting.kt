package formulaide.server.routes

import formulaide.api.rest.toRest
import formulaide.api.utils.flatMapSuccesses
import formulaide.api.utils.mapSuccesses
import formulaide.core.Department
import formulaide.core.User
import formulaide.server.Auth.Companion.Employee
import formulaide.server.api2
import formulaide.server.context
import formulaide.server.database
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import opensavvy.backbone.Ref.Companion.request
import opensavvy.backbone.Ref.Companion.requestValueOrThrow
import opensavvy.spine.ktor.server.route
import opensavvy.state.firstValueOrThrow
import opensavvy.state.slice.ensureAuthorized
import opensavvy.state.slice.successful

/**
 * Department management.
 *
 * This endpoint lives at `/api/departments` and is used to manage [company departments][Department].
 */
@Suppress("MemberVisibilityCanBePrivate")
object DepartmentRouting {

	internal fun Routing.enable() = authenticate(Employee, optional = true) {
		route(api2.departments.get, context) {
			val result = database.departments.all(parameters.includeClosed)
				.mapSuccesses { list ->
					list.map { api2.departments.id.idOf(it.id) }
				}.firstValueOrThrow()

			result
		}

		route(api2.departments.id.get, context) {
			val result = database.departments.fromId(id)
				.request()
				.flatMapSuccesses {
					if (!it.open)
						ensureAuthorized(context.role >= User.Role.ADMINISTRATOR) { "Seuls les administrateurs peuvent accéder à des départements fermés" }

					emit(successful(it.toRest()))
				}

			result.firstValueOrThrow()
		}

		route(api2.departments.id.open, context) {
			val ref = database.departments.fromId(id)
			val result = database.departments.open(ref)
			result.firstValueOrThrow()
		}

		route(api2.departments.id.close, context) {
			val ref = database.departments.fromId(id)
			val result = database.departments.close(ref)
			result.firstValueOrThrow()
		}

		route(api2.departments.create, context) {
			val result = database.departments.create(body.name)
				.mapSuccesses { api2.departments.id.idOf(it.id) to it.requestValueOrThrow().toRest() }

			result.firstValueOrThrow()
		}
	}
}
