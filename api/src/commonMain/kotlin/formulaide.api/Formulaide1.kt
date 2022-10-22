package formulaide.api

import formulaide.api.rest.RestDepartment
import formulaide.core.User
import opensavvy.spine.Id
import opensavvy.spine.Parameters
import opensavvy.spine.Route
import opensavvy.spine.Route.Companion.div
import opensavvy.spine.Service
import opensavvy.state.StateBuilder
import opensavvy.state.ensureAuthenticated
import opensavvy.state.ensureAuthorized

class Formulaide1 : Service("v1") {

	//region Departments

	inner class DepartmentsEndpoint : StaticResource<List<Id>, RestDepartment.GetParams, Context>("departments") {

		override suspend fun StateBuilder<Nothing>.validateId(id: Id, context: Context) {
			ensureAuthenticated(context.role >= User.Role.EMPLOYEE) { "Seuls les employés peuvent accéder aux départements" }
		}

		override suspend fun StateBuilder<List<Id>>.validateGetParams(
			id: Id,
			params: RestDepartment.GetParams,
			context: Context,
		) {
			if (params.includeClosed) {
				ensureAuthorized(context.role >= User.Role.ADMINISTRATOR) { "Seuls les administrateurs peuvent accéder aux départements fermés" }
			}
		}

		inner class DepartmentEndpoint : DynamicResource<RestDepartment, Context>("department") {

			val open = action<Unit, Unit, Parameters.Empty>(Route / "open") { _, _, _, context ->
				ensureAuthorized(context.role >= User.Role.ADMINISTRATOR) { "Seuls les administrateurs peuvent rouvrir un département" }
			}

			val close = action<Unit, Unit, Parameters.Empty>(Route / "close") { _, _, _, context ->
				ensureAuthorized(context.role >= User.Role.ADMINISTRATOR) { "Seuls les administrateurs peuvent fermer un département" }
			}

		}

		val create = create<RestDepartment.New, RestDepartment, Parameters.Empty> { _, _, _, context ->
			ensureAuthorized(context.role >= User.Role.ADMINISTRATOR) { "Seuls les administrateurs peuvent créer des départements" }
		}

		val id = DepartmentEndpoint()
	}

	val departments = DepartmentsEndpoint()

	//endregion
}
