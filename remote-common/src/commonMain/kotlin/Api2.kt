package opensavvy.formulaide.remote

import arrow.core.identity
import arrow.core.raise.either
import opensavvy.formulaide.core.utils.Identifier
import opensavvy.formulaide.remote.Api2.*
import opensavvy.formulaide.remote.Api2.DepartmentsEndpoint.DepartmentEndpoint
import opensavvy.formulaide.remote.Api2.FormsEndpoint.FormEndpoint
import opensavvy.formulaide.remote.Api2.FormsEndpoint.FormEndpoint.FormVersionEndpoint
import opensavvy.formulaide.remote.Api2.RecordsEndpoint.RecordEndpoint
import opensavvy.formulaide.remote.Api2.SubmissionsEndpoint.SubmissionEndpoint
import opensavvy.formulaide.remote.Api2.TemplatesEndpoint.TemplateEndpoint
import opensavvy.formulaide.remote.Api2.TemplatesEndpoint.TemplateEndpoint.TemplateVersionEndpoint
import opensavvy.formulaide.remote.Api2.UsersEndpoint.UserEndpoint
import opensavvy.formulaide.remote.Api2.UsersEndpoint.UserEndpoint.*
import opensavvy.formulaide.remote.dto.*
import opensavvy.spine.*
import opensavvy.spine.Route.Companion.div

val api = Api2()

/**
 * The Formulaide 2.0 API.
 *
 * ### Resources
 *
 * - [Departments][DepartmentsEndpoint]
 * - [Users][UsersEndpoint]
 * - [Templates][TemplatesEndpoint]
 * - [Forms][FormsEndpoint]
 * - [Submissions][SubmissionsEndpoint]
 * - [Records][RecordsEndpoint]
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
	 * - Query parameters: [DepartmentDto.ListParams]
	 * - Response: list of identifiers of the various departments
	 *
	 * Authorization:
	 * - Without parameters: employee
	 * - With [DepartmentDto.ListParams.includeClosed] set to `true`: administrator
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
	inner class DepartmentsEndpoint : StaticResource<List<Id>, DepartmentDto.ListFailures, DepartmentDto.ListParams, Unit>("departments") {

		val create = create<DepartmentDto.New, DepartmentDto.NewFailures, Unit, Parameters.Empty>()

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
		inner class DepartmentEndpoint : DynamicResource<DepartmentDto, DepartmentDto.GetFailures, Unit>("department") {

			val edit = edit<DepartmentDto.Edit, DepartmentDto.EditFailures, Parameters.Empty>()

			suspend fun identifierOf(id: Id) = run {
				validateIdOrThrow(id.takeFirst(2), Unit)
				Identifier(id.resource.segments.last().segment)
			}

		}

		val id = DepartmentEndpoint()
	}

	val departments = DepartmentsEndpoint()

	//endregion
	//region Users

	/**
	 * The user management endpoint: `v2/users`.
	 *
	 * ### Get
	 *
	 * Lists existing users.
	 *
	 * - Query parameters: [UserDto.ListParams]
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
	inner class UsersEndpoint : StaticResource<List<Id>, UserDto.ListFailures, UserDto.ListParams, Unit>("users") {

		val create = create<UserDto.New, UserDto.NewFailures, String, Parameters.Empty>()

		val logIn = create<UserDto.LogIn, UserDto.LogInFailures, String, Parameters.Empty>(Route / "token")

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
		inner class UserEndpoint : DynamicResource<UserDto, UserDto.GetFailures, Unit>("user") {

			val edit = edit<UserDto.Edit, UserDto.EditFailures, Parameters.Empty>()

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
			inner class DepartmentEndpoint : StaticResource<Set<Id>, UserDto.DepartmentListFailures, Parameters.Empty, Unit>("departments") {
				val add = action<Id, UserDto.DepartmentAddFailures, Unit, Parameters.Empty>(Route.Root)
				val remove = delete<Id, UserDto.DepartmentRemoveFailures>(Route.Root)
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
			inner class PasswordEndpoint : StaticResource<Unit, Unit, Parameters.Empty, Unit>("password") {
				val reset = action<Unit, UserDto.PasswordResetFailures, String, Parameters.Empty>(Route.Root)
				val set = create<UserDto.SetPassword, UserDto.PasswordSetFailures, Unit, Parameters.Empty>()
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
			inner class TokenEndpoint : StaticResource<Unit, Unit, Parameters.Empty, Unit>("token") {
				val verify = action<String, UserDto.TokenVerifyFailures, Unit, Parameters.Empty>(Route.Root)
				val logOut = delete<String, UserDto.LogOutFailures>()
			}

			suspend fun identifierOf(id: Id) = run {
				validateIdOrThrow(id.takeFirst(2), Unit)
				Identifier(id.resource.segments[1].segment)
			}

			val departments = DepartmentEndpoint()
			val password = PasswordEndpoint()
			val token = TokenEndpoint()
		}

		val id = UserEndpoint()
	}

	val users = UsersEndpoint()

	//endregion
	//region Templates

	/**
	 * The template management endpoint: `v2/templates`.
	 *
	 * ### Get
	 *
	 * List existing templates.
	 *
	 * - Parameters: [SchemaDto.ListParams]
	 * - Response: list of identifiers
	 *
	 * Authorization: employee
	 *
	 * ### Post
	 *
	 * Creates a new template.
	 *
	 * - Body: [SchemaDto.New]
	 * - Response: identifier of the created template
	 *
	 * Authorization: administrator
	 *
	 * ### Sub-resources
	 *
	 * - Individual template: [TemplateEndpoint]
	 */
	inner class TemplatesEndpoint : StaticResource<List<Id>, SchemaDto.ListFailures, SchemaDto.ListParams, Unit>("templates") {

		val create = create<SchemaDto.New, SchemaDto.NewFailures, Unit, Parameters.Empty>()

		/**
		 * The template management endpoint: `v2/templates/{id}`.
		 *
		 * ### Get
		 *
		 * Access detailed information of a template.
		 *
		 * - Response: [SchemaDto]
		 *
		 * Authorization: employee
		 *
		 * ### Post
		 *
		 * Creates a new version of this template.
		 *
		 * - Body: [SchemaDto.Version]
		 * - Response: identifier of the created version
		 *
		 * Authorization: administrator
		 *
		 * ### Patch
		 *
		 * Edits this template.
		 *
		 * - Body: [SchemaDto.Edit]
		 *
		 * Authorization: administrator
		 *
		 * ### Sub-resources
		 *
		 * - Template versions: [TemplateVersionEndpoint]
		 */
		inner class TemplateEndpoint : DynamicResource<SchemaDto, SchemaDto.GetFailures, Unit>("template") {

			val create = create<SchemaDto.NewVersion, SchemaDto.NewFailures, Unit, Parameters.Empty>()

			val edit = edit<SchemaDto.Edit, SchemaDto.EditFailures, Parameters.Empty>()

			suspend fun identifierOf(id: Id) = run {
				validateIdOrThrow(id.takeFirst(2), Unit)
				Identifier(id.resource.segments[1].segment)
			}

			/**
			 * The template version management endpoint: `v2/templates/{id}/{version}`.
			 *
			 * ### Get
			 *
			 * Access detailed information about this version.
			 *
			 * - Response: [SchemaDto.Version]
			 *
			 * Authorization: employee
			 */
			inner class TemplateVersionEndpoint : DynamicResource<SchemaDto.Version, SchemaDto.GetVersionFailures, Unit>("version") {

				suspend fun identifierOf(id: Id) = run {
					validateIdOrThrow(id.takeFirst(3), Unit)
					Identifier(id.resource.segments.takeLast(2).joinToString("_"))
				}
			}

			val version = TemplateVersionEndpoint()
		}

		val id = TemplateEndpoint()
	}

	val templates = TemplatesEndpoint()

	//endregion
	//region Forms

	/**
	 * The form management endpoint: `v2/forms`.
	 *
	 * ### Get
	 *
	 * List existing forms.
	 *
	 * - Parameters: [SchemaDto.ListParams]
	 * - Response: list of identifiers
	 *
	 * Authorization:
	 * - Guests will only see public forms
	 * - Employees will see all forms
	 *
	 * ### Post
	 *
	 * Creates a new form.
	 *
	 * - Body: [SchemaDto.New]
	 * - Response: identifier of the created form
	 *
	 * Authorization: administrator
	 *
	 * ### Sub-resources
	 *
	 * - Individual form: [FormEndpoint]
	 */
	inner class FormsEndpoint : StaticResource<List<Id>, SchemaDto.ListFailures, SchemaDto.ListParams, Unit>("forms") {

		val create = create<SchemaDto.New, SchemaDto.NewFailures, Unit, Parameters.Empty>()

		/**
		 * The form management endpoint: `v2/forms/{id}`.
		 *
		 * ### Get
		 *
		 * Access detailed information of a form.
		 *
		 * - Response: [SchemaDto]
		 *
		 * Authorization:
		 * - If this form is public: guest
		 * - If this form is private: employee
		 *
		 * ### Post
		 *
		 * Creates a new version of this form.
		 *
		 * - Body: [SchemaDto.Version]
		 * - Response: identifier of the created version
		 *
		 * Authorization: administrator
		 *
		 * ### Patch
		 *
		 * Edits this template.
		 *
		 * - Body: [SchemaDto.Edit]
		 *
		 * Authorization: administrator
		 *
		 * ### Sub-resources
		 *
		 * - Form versions: [FormVersionEndpoint]
		 */
		inner class FormEndpoint : DynamicResource<SchemaDto, SchemaDto.GetFailures, Unit>("form") {

			val create = create<SchemaDto.NewVersion, SchemaDto.NewFailures, Unit, Parameters.Empty>()

			val edit = edit<SchemaDto.Edit, SchemaDto.EditFailures, Parameters.Empty>()

			suspend fun identifierOf(id: Id) = run {
				validateIdOrThrow(id.takeFirst(2), Unit)
				Identifier(id.resource.segments[1].segment)
			}

			/**
			 * The form version management endpoint: `v2/forms/{id}/{version}`.
			 *
			 * ### Get
			 *
			 * Access detailed information about this version.
			 *
			 * - Response: [SchemaDto.Version]
			 *
			 * Authorization:
			 * - If this form is public: guest
			 * - If this form is private: employee
			 */
			inner class FormVersionEndpoint : DynamicResource<SchemaDto.Version, SchemaDto.GetVersionFailures, Unit>("version") {

				suspend fun identifierOf(id: Id) = run {
					validateIdOrThrow(id.takeFirst(3), Unit)
					Identifier(id.resource.segments.takeLast(2).joinToString("_"))
				}
			}

			val version = FormVersionEndpoint()
		}

		val id = FormEndpoint()
	}

	val forms = FormsEndpoint()

	//endregion
	//region Submissions and records

	/**
	 * The submissions management endpoint: `v2/submissions`.
	 *
	 * Submissions are managed through the [records][RecordsEndpoint] endpoint.
	 *
	 * ### Sub-resources
	 *
	 * - Access a specific submission: [SubmissionEndpoint]
	 */
	inner class SubmissionsEndpoint : StaticResource<Nothing, Unit, Parameters.Empty, Unit>("submissions") {

		/**
		 * The submission endpoint: `v2/submissions/{id}`.
		 *
		 * ### Get
		 *
		 * Accesses a specific submission.
		 * - Response: [SubmissionDto]
		 *
		 * Authorization: employee
		 */
		inner class SubmissionEndpoint : DynamicResource<SubmissionDto, SubmissionDto.GetFailures, Unit>("submission") {

			suspend fun identifierOf(id: Id) = run {
				validateIdOrThrow(id.takeFirst(2), Unit)
				Identifier(id.resource.segments.last().segment)
			}
		}

		val id = SubmissionEndpoint()
	}

	/**
	 * The records management endpoint: `v2/records`.
	 *
	 * ### Put
	 *
	 * Searches for submissions.
	 * - Body: list of [RecordDto.Criterion]
	 * - Response: list of identifiers of found records
	 *
	 * Authorization: employee
	 *
	 * ### Create
	 *
	 * Creates a new submission to a form.
	 * - Body: [SubmissionDto]
	 * - Response: identifier of the created record and [SubmissionDto]
	 *
	 * You should ensure that the returned submission is identical to the one you sent.
	 * If they are different, it means some provided fields were not recognized.
	 *
	 * Authorization:
	 * - guest if the form is public,
	 * - employee if the form is private.
	 *
	 * ### Sub-resources
	 *
	 * - Access individual records: [RecordEndpoint]
	 */
	inner class RecordsEndpoint : StaticResource<Nothing, Unit, Parameters.Empty, Unit>("records") {

		val search = action<List<RecordDto.Criterion>, RecordDto.SearchFailures, List<Id>, Parameters.Empty>(Route.Root)

		val create = create<SubmissionDto, RecordDto.NewFailures, SubmissionDto, Parameters.Empty>()

		/**
		 * The record management endpoint: `v2/records/{id}`.
		 *
		 * ### Get
		 *
		 * Accesses detailed information about this record.
		 * - Response: [RecordDto]
		 *
		 * Authorization: employee
		 *
		 * ### Put /advance
		 *
		 * Advances this record to another state.
		 * - Body: [RecordDto.Advance]
		 *
		 * Authorization: employee
		 */
		inner class RecordEndpoint : DynamicResource<RecordDto, RecordDto.GetFailures, Unit>("record") {

			suspend fun identifierOf(id: Id) = run {
				validateIdOrThrow(id.takeFirst(2), Unit)
				Identifier(id.resource.segments.last().segment)
			}

			val advance = action<RecordDto.Advance, RecordDto.AdvanceFailures, Unit, Parameters.Empty>(Route / "advance")

		}

		val id = RecordEndpoint()
	}

	val submissions = SubmissionsEndpoint()
	val records = RecordsEndpoint()

	//endregion

}

// region Utils

private suspend fun <O : Any, Context : Any> ResourceGroup.AbstractResource<O, Context>.validateIdOrThrow(id: Id, context: Context) = either {
	validateCorrectId(id)
	validateId(id, context)
}.fold(
	ifLeft = { error("Invalid identifier for $this: $id") },
	ifRight = ::identity,
)

private fun Id.takeFirst(resources: Int) = copy(
	service = service,
	resource = Route(resource.segments.take(resources)),
)

// endregion
