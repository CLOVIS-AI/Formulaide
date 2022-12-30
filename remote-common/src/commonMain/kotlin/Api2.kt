package opensavvy.formulaide.remote

import opensavvy.formulaide.core.Department
import opensavvy.formulaide.remote.Api2.DepartmentsEndpoint
import opensavvy.formulaide.remote.Api2.DepartmentsEndpoint.DepartmentEndpoint
import opensavvy.formulaide.remote.dto.DepartmentDto
import opensavvy.spine.Id
import opensavvy.spine.Parameters
import opensavvy.spine.Service
import opensavvy.state.slice.Slice
import opensavvy.state.slice.slice

val api = Api2()

/**
 * The Formulaide 2.0 API.
 *
 * ### Resources
 *
 * - [Departments][DepartmentsEndpoint]
 */
class Api2 : Service("v2") {

	//region Departments

	/**
	 * The department management endpoint: `v2/departments`.
	 *
	 * ### Get
	 *
	 * Lists the existing departments.
	 *
	 * - Query parameters: [DepartmentDto.GetParams]
	 * - Response: list of identifiers of the various departments
	 *
	 * Authorization:
	 * - Without parameters: employee
	 * - With [DepartmentDto.GetParams.includeClosed] set to `true`: administrator
	 *
	 * ### Post
	 *
	 * Creates a new department.
	 * - Body: [DepartmentDto.New]
	 * - Response: identifier of the created department
	 *
	 * Authorization: administrator
	 *
	 * ### Sub-resources
	 *
	 * - Edit a specific department: [DepartmentEndpoint]
	 */
	inner class DepartmentsEndpoint : StaticResource<List<Id>, DepartmentDto.GetParams, Unit>("departments") {

		val create = create<DepartmentDto.New, Unit, Parameters.Empty>()

		/**
		 * The department management endpoint: `v2/departments/{id}`.
		 *
		 * ### Get
		 *
		 * Accesses a specific department.
		 * - Response: [DepartmentDto]
		 *
		 * Authorization:
		 * - if the department is open: employee
		 * - if the department is closed: administrator
		 *
		 * ### Patch
		 *
		 * Edits the department.
		 * - Body: [DepartmentDto.Edit]
		 *
		 * Authorization: administrator
		 */
		inner class DepartmentEndpoint : DynamicResource<DepartmentDto, Unit>("department") {

			val edit = edit<DepartmentDto.Edit, Parameters.Empty>()

			suspend fun refOf(id: Id, departments: Department.Service): Slice<Department.Ref> = slice {
				validateId(id, Unit)
				Department.Ref(id.resource.segments.last().segment, departments)
			}

		}

		val id = DepartmentEndpoint()
	}

	val departments = DepartmentsEndpoint()

	//endregion

}
