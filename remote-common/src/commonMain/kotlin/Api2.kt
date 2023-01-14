package opensavvy.formulaide.remote

import opensavvy.formulaide.core.Department
import opensavvy.formulaide.core.User
import opensavvy.formulaide.remote.Api2.DepartmentsEndpoint
import opensavvy.formulaide.remote.Api2.DepartmentsEndpoint.DepartmentEndpoint
import opensavvy.formulaide.remote.Api2.UsersEndpoint
import opensavvy.formulaide.remote.Api2.UsersEndpoint.UserEndpoint
import opensavvy.formulaide.remote.Api2.UsersEndpoint.UserEndpoint.*
import opensavvy.formulaide.remote.dto.DepartmentDto
import opensavvy.formulaide.remote.dto.UserDto
import opensavvy.spine.Id
import opensavvy.spine.Parameters
import opensavvy.spine.Route
import opensavvy.spine.Route.Companion.div
import opensavvy.spine.Service
import opensavvy.state.outcome.Outcome
import opensavvy.state.outcome.out

val api = Api2()

/**
 * The Formulaide 2.0 API.
 *
 * ### Resources
 *
 * - [Departments][DepartmentsEndpoint]
 * - [Users][UsersEndpoint]
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

			suspend fun refOf(id: Id, departments: Department.Service): Outcome<Department.Ref> = out {
				validateId(id, Unit)
				Department.Ref(id.resource.segments.last().segment, departments)
			}

		}

		val id = DepartmentEndpoint()
	}

	val departments = DepartmentsEndpoint()

	//endregion
	//region

	/**
	 * The user management endpoint: `v2/users`.
	 *
	 * ### Get
	 *
	 * Lists existing users.
	 *
	 * - Query parameters: [UserDto.GetParams]
	 * - Response: list of identifiers of the various users
	 *
	 * Authorization: administrator
	 *
	 * ### POST
	 *
	 * Creates a new user.
	 *
	 * - Body: [UserDto.New]
	 * - Response: identifier of the created user and a single-use password
	 *
	 * Authorization: administrator
	 *
	 * ### POST /token
	 *
	 * Request a new token using a username and a password.
	 *
	 * - Body: [UserDto.LogIn]
	 * - Response: identifier of the user and a token
	 *
	 * Authorization: guest
	 *
	 * ### Sub-resources
	 *
	 * - Manage a specific user: [UserEndpoint]
	 */
	inner class UsersEndpoint : StaticResource<List<Id>, UserDto.GetParams, Unit>("users") {

		val create = create<UserDto.New, String, Parameters.Empty>()

		val logIn = create<UserDto.LogIn, String, Parameters.Empty>(Route / "token")

		/**
		 * The individual user management endpoint: `v2/users/{id}`.
		 *
		 * ### Get
		 *
		 * Accesses detailed information about the specified user.
		 *
		 * - Response: [UserDto]
		 *
		 * Authorization: employee
		 *
		 * ### Patch
		 *
		 * Edits information about the specified user.
		 *
		 * - Body: [UserDto.Edit]
		 *
		 * Authorization: administrator
		 *
		 * ### Sub-resources
		 *
		 * - Department management: [DepartmentEndpoint]
		 * - Password management: [PasswordEndpoint]
		 * - Token management: [TokenEndpoint]
		 */
		inner class UserEndpoint : DynamicResource<UserDto, Unit>("user") {

			val edit = edit<UserDto.Edit, Parameters.Empty>()

			/**
			 * The user department management endpoint: `v2/users/{id}/departments`.
			 *
			 * ### Get
			 *
			 * Get the departments this user is a part of.
			 *
			 * - Response: list of department identifiers
			 *
			 * Authorization: administrator
			 *
			 * ### Put
			 *
			 * Adds this user to a department.
			 *
			 * - Body: identifier of the department
			 *
			 * Authorization: administrator
			 *
			 * ### Delete
			 *
			 * Removes this user from a department.
			 *
			 * - Body: identifier of the department
			 *
			 * Authorization: administrator
			 */
			inner class DepartmentEndpoint : StaticResource<Set<Id>, Parameters.Empty, Unit>("departments") {
				val add = action<Id, Unit, Parameters.Empty>(Route.Root)
				val remove = delete<Id>(Route.Root)
			}

			/**
			 * The password management endpoint: `v2/users/{id}/password`.
			 *
			 * ### Put
			 *
			 * Resets the user's password.
			 *
			 * - Response: single-use password
			 *
			 * Authorization: administrator
			 *
			 * ### Post
			 *
			 * Replaces the user's password.
			 *
			 * - Body: [UserDto.SetPassword]
			 *
			 * Authorization: employee
			 */
			inner class PasswordEndpoint : StaticResource<Unit, Parameters.Empty, Unit>("password") {
				val reset = action<Unit, String, Parameters.Empty>(Route.Root)
				val set = create<UserDto.SetPassword, Unit, Parameters.Empty>()
			}

			/**
			 * The token management endpoint: `v2/users/{id}/token`.
			 *
			 * ### Post
			 *
			 * Verifies whether a token is a valid for this user.
			 *
			 * - Body: the token
			 *
			 * Authorization: guest
			 *
			 * ### Delete
			 *
			 * Logs out the user (destroys their token).
			 *
			 * - Body: the token
			 *
			 * Authorization: employee
			 */
			inner class TokenEndpoint : StaticResource<Unit, Parameters.Empty, Unit>("token") {
				val verify = action<String, Unit, Parameters.Empty>(Route.Root)
				val logOut = delete<String>()
			}

			suspend fun refOf(id: Id, users: User.Service): Outcome<User.Ref> = out {
				validateId(id, Unit)
				User.Ref(id.resource.segments[1].segment, users)
			}

			val departments = DepartmentEndpoint()
			val password = PasswordEndpoint()
			val token = TokenEndpoint()
		}

		val id = UserEndpoint()
	}

	val users = UsersEndpoint()

	//endregion

}
