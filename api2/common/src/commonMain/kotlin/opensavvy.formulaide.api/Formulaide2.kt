package opensavvy.formulaide.api

import opensavvy.formulaide.api.Formulaide2.DepartmentsEndpoint.DepartmentEndpoint
import opensavvy.spine.Id
import opensavvy.spine.Parameters
import opensavvy.spine.Route
import opensavvy.spine.Route.Companion.div
import opensavvy.spine.Service
import opensavvy.state.Slice.Companion.successful
import opensavvy.state.firstResult
import opensavvy.state.state

val api2 = Formulaide2()

/**
 * The Formulaide 2.0 API.
 */
class Formulaide2 : Service("v2") {

	//region Departments

	/**
	 * The department collection management endpoint: `/v2/departments`.
	 *
	 * ### GET
	 *
	 * Lists the existing departments.
	 *
	 * - Query parameters: [Department.GetParams]
	 * - Response: list of identifiers of the various departments
	 *
	 * Authorization:
	 * - Without parameters: employee
	 * - With [Department.GetParams.includeClosed] set to `true`: administrator
	 *
	 * ### POST
	 *
	 * Creates a new department.
	 * - Body: [Department.New]
	 * - Response: ID of the created department
	 *
	 * Authorization: administrator
	 *
	 * ### Sub-resources
	 *
	 * - Edit a specific department: [DepartmentEndpoint].
	 */
	inner class DepartmentsEndpoint : StaticResource<List<Id>, Department.GetParams, Context>("departments") {

		val create = create<Department.New, Unit, Parameters.Empty>()

		/**
		 * The department management endpoint: `/v2/departments/{id}`.
		 *
		 * ### GET
		 *
		 * Accesses a specific department.
		 *
		 * - Response: [Department]
		 *
		 * Authorization:
		 * - If the department is open: employee
		 * - If the department is closed: administrator
		 *
		 * ### PATCH /open
		 *
		 * Opens or closes the department.
		 *
		 * - Body: [Department.EditVisibility]
		 *
		 * Authorization: administrator
		 */
		inner class DepartmentEndpoint : DynamicResource<Department, Context>("department") {

			val visibility = edit<Department.EditVisibility, Parameters.Empty>(Route / "open")

			suspend fun idFrom(id: Id, context: Context) = state {
				validateId(id, context)
				emit(successful(id.resource.segments.last().segment))
			}.firstResult()

		}

		val id = DepartmentEndpoint()
	}

	val departments = DepartmentsEndpoint()

	//endregion

}
