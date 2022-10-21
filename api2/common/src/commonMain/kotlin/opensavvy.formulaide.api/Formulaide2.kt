package opensavvy.formulaide.api

import opensavvy.formulaide.api.Formulaide2.DepartmentsEndpoint.DepartmentEndpoint
import opensavvy.formulaide.api.Formulaide2.UsersEndpoint.MeEndpoint
import opensavvy.formulaide.api.Formulaide2.UsersEndpoint.UserEndpoint
import opensavvy.formulaide.state.bind
import opensavvy.spine.Id
import opensavvy.spine.Parameters
import opensavvy.spine.Route
import opensavvy.spine.Route.Companion.div
import opensavvy.spine.Service
import opensavvy.state.Slice.Companion.successful
import opensavvy.state.ensureValid
import opensavvy.state.firstResult
import opensavvy.state.state

val api2 = Formulaide2()

/**
 * The Formulaide 2.0 API.
 */
class Formulaide2 : Service("v2") {

	//region Ping

	inner class PingEndpoint : StaticResource<Unit, Parameters.Empty, Context>("ping")

	val ping = PingEndpoint()

	//endregion
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
	//region Users

	/**
	 * The user collection management endpoint: `/v2/users`.
	 *
	 * ### GET
	 *
	 * Lists the existing users.
	 *
	 * - Query parameters: [User.GetParams]
	 * - Response: list of identifiers of the various users
	 *
	 * Authorization: administrator
	 *
	 * ### POST
	 *
	 * Creates a new user.
	 *
	 * - Body: [User.New]
	 * - Response: ID of the created user and the generated [User.TemporaryPassword]
	 *
	 * Authorization: administrator
	 *
	 * ### Sub-resources
	 *
	 * - Access a specific user: [UserEndpoint]
	 * - Access the user currently logged-in: [MeEndpoint]
	 */
	inner class UsersEndpoint : StaticResource<List<Id>, User.GetParams, Context>("users") {

		val create = create<User.New, User.TemporaryPassword, Parameters.Empty> { _, input, _, _ ->
			ensureValid(input.name.length > 2) { "Ce nom est trop court : '${input.name}'" }
			ensureValid(input.name.length < 50) { "Ce nom est trop long : '${input.name}'" }

			ensureValid('@' in input.email) { "L'adresse mail est invalide : '${input.email}'" }
		}

		/**
		 * Accesses detailed information about a user: `v2/users/{id}`.
		 *
		 * ### GET
		 *
		 * Accesses detailed information about a user.
		 *
		 * - Response: [User]
		 *
		 * Authorization: employee
		 *
		 * ### PATCH
		 *
		 * Edits the specified user.
		 *
		 * - Body: [User.Edit]
		 *
		 * Authorization: administrator
		 *
		 * ### PUT /resetPassword
		 *
		 * Resets the user's password.
		 *
		 * - Response: [User.TemporaryPassword]
		 *
		 * Authorization: administrator
		 */
		inner class UserEndpoint : DynamicResource<User, Context>("user") {

			val edit = edit<User.Edit, Parameters.Empty> { id, body, _, context ->
				val targetUserId = bind(idFrom(id, context))
				val myUserId = context.user?.id

				if (body.open != null || body.administrator != null)
					ensureValid(targetUserId != myUserId) { "Il est interdit de modifier son propre statut (clôturer le compte) et ses droits (administrateur ou non)" }
			}

			val resetPassword = action<Unit, User.TemporaryPassword, Parameters.Empty>(Route / "resetPassword")

			suspend fun idFrom(id: Id, context: Context) = state {
				validateId(id, context)
				emit(successful(id.resource.segments.last().segment))
			}.firstResult()
		}

		/**
		 * Accesses the current user: `/v2/users/me`.
		 *
		 * ### GET
		 *
		 * Get the ID of the currently logged-in user.
		 *
		 * Authorization: employee
		 *
		 * ### PATCH /password
		 *
		 * Edits my password.
		 *
		 * For security reasons, upon success, all current sessions for this user will be invalidated.
		 * It will be necessary to log in again (with the new password).
		 *
		 * - Body: [User.PasswordModification]
		 *
		 * Authorization: employee
		 *
		 * ### POST
		 *
		 * Logs in.
		 *
		 * - Body: [User.LogInForm]
		 * - The token is inserted by the server into the cookies directly, there is nothing to do
		 *
		 * Authorization: none
		 *
		 * ### DELETE
		 *
		 * Logs out.
		 *
		 * - The token is removed by the server directly, there is nothing to do
		 *
		 * Authorization: employee
		 */
		inner class MeEndpoint : StaticResource<Id, Parameters.Empty, Context>("me") {

			val editPassword = edit<User.PasswordModification, Parameters.Empty>(Route / "password") { _, input, _, _ ->
				val minPasswordSize = 9
				ensureValid(input.newPassword.length >= minPasswordSize) { "Le mot de passe demandé est trop court : ${input.newPassword.length} caractères fournis mais $minPasswordSize sont nécessaires" }

				val blacklist = listOf(
					"00000000",
					"12345678",
					"123456789",
				)
				ensureValid(input.newPassword !in blacklist) { "Le mot de passe choisi est trop simple à deviner." }
			}

			val logIn = create<User.LogInForm, Unit, Parameters.Empty>()

			val logOut = delete<Unit>()

		}

		val id = UserEndpoint()
		val me = MeEndpoint()
	}

	val users = UsersEndpoint()

	//endregion

}
