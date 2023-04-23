package opensavvy.formulaide.core

import opensavvy.backbone.Backbone
import opensavvy.formulaide.core.data.Email
import opensavvy.formulaide.core.data.Password
import opensavvy.formulaide.core.data.Token
import opensavvy.state.failure.CustomFailure
import opensavvy.state.failure.Failure
import opensavvy.state.failure.Unauthenticated
import opensavvy.state.failure.Unauthorized
import opensavvy.state.outcome.Outcome
import kotlin.jvm.JvmName

/**
 * A user of the project.
 *
 * Formulaide is built so end-users do not require accounts.
 * This class therefore only represents real users of the application.
 *
 * Only administrators can create new accounts.
 */
data class User(
	/**
	 * The email address of this user.
	 */
	val email: Email,

	/**
	 * The name of this user.
	 */
	val name: String,

	/**
	 * Whether this user is active.
	 *
	 * It is not possible to log in as an inactive user.
	 * Only administrators can see inactive users.
	 */
	val active: Boolean,

	/**
	 * The role of this user.
	 */
	val administrator: Boolean,

	/**
	 * The departments this user is a part of.
	 *
	 * The user can see submissions to forms and templates assigned to any of these departments.
	 */
	val departments: Set<Department.Ref>,

	/**
	 * If `true`, this user is currently limited to a single-use password.
	 *
	 * After authenticating themselves, they will be forced to select a new password.
	 */
	val singleUsePassword: Boolean,
) {

	enum class Role {
		Guest,
		Employee,
		Administrator,
		;

		companion object {

			val User.role
				get() = when (administrator) {
					true -> Administrator
					false -> Employee
				}

			@get:JvmName("getRoleNullable")
			val User?.role get() = this?.role ?: Guest
		}
	}

	interface Ref : opensavvy.backbone.Ref<Failures.Get, User> {

		/**
		 * Joins [department].
		 *
		 * @see User.departments
		 */
		suspend fun join(department: Department.Ref): Outcome<Failures.Edit, Unit>

		/**
		 * Leaves [department].
		 *
		 * @see User.departments
		 */
		suspend fun leave(department: Department.Ref): Outcome<Failures.Edit, Unit>

		/**
		 * Enables this user.
		 *
		 * @see User.active
		 */
		suspend fun enable(): Outcome<Failures.SecurityEdit, Unit>

		/**
		 * Disables this user.
		 *
		 * @see User.active
		 */
		suspend fun disable(): Outcome<Failures.SecurityEdit, Unit>

		/**
		 * Promotes this user to [Role.Administrator].
		 *
		 * @see User.administrator
		 */
		suspend fun promote(): Outcome<Failures.SecurityEdit, Unit>

		/**
		 * Demotes this user to [Role.Employee].
		 *
		 * @see User.administrator
		 */
		suspend fun demote(): Outcome<Failures.SecurityEdit, Unit>

		/**
		 * Resets this user's password.
		 *
		 * @see User.singleUsePassword
		 */
		suspend fun resetPassword(): Outcome<Failures.Edit, Password>

		/**
		 * Sets this user's password.
		 *
		 * @see User.singleUsePassword
		 */
		suspend fun setPassword(oldPassword: String, newPassword: Password): Outcome<Failures.SetPassword, Unit>

		/**
		 * Verifies this [token].
		 */
		suspend fun verifyToken(token: Token): Outcome<Failures.TokenVerification, Unit>

		/**
		 * Invalidates this [token].
		 */
		suspend fun logOut(token: Token): Outcome<Failures.Get, Unit>
	}

	interface Service<R : Ref> : Backbone<R, Failures.Get, User> {

		/**
		 * Lists all users.
		 *
		 * Requires administrator authentication.
		 */
		suspend fun list(includeClosed: Boolean = false): Outcome<Failures.List, List<Ref>>

		/**
		 * Creates a new user.
		 *
		 * Requires administrator authentication.
		 *
		 * @return A reference to the created user, as well as their generated single-use password.
		 */
		suspend fun create(
			email: Email,
			fullName: String,
			administrator: Boolean = false,
		): Outcome<Failures.Create, Pair<Ref, Password>>

		/**
		 * Checks that [email] and [password] match an existing user.
		 *
		 * @return The reference to the logged-in user, and a valid connection token.
		 */
		suspend fun logIn(
			email: Email,
			password: Password,
		): Outcome<Failures.LogIn, Pair<Ref, Token>>
	}

	sealed interface Failures : Failure {
		sealed interface Get : Failures
		sealed interface List : Failures
		sealed interface Create : Failures
		sealed interface Edit : Failures
		sealed interface SecurityEdit : Failures
		sealed interface LogIn : Failures
		sealed interface TokenVerification : Failures
		sealed interface SetPassword : Failures

		class NotFound(val ref: Ref) : CustomFailure(opensavvy.state.failure.NotFound(ref)),
			Get,
			Create,
			Edit,
			SecurityEdit

		object Unauthenticated : CustomFailure(Unauthenticated()),
			Get,
			Create,
			Edit,
			SecurityEdit,
			List,
			SetPassword

		object Unauthorized : CustomFailure(Unauthorized()),
			Get,
			Create,
			Edit,
			SecurityEdit,
			List

		class UserAlreadyExists(val email: Email) : CustomFailure(Companion, "The user '$email' already exists"),
			Create {
			companion object : Failure.Key
		}

		object CannotEditYourself : CustomFailure(Unauthorized("Editing yourself is forbidden")),
			SecurityEdit

		object CanOnlySetYourOwnPassword : CustomFailure(Unauthorized("Setting another user's password is forbidden")),
			SetPassword

		class IncorrectPassword : CustomFailure(Companion, "The provided password is incorrect"),
			SetPassword {
			companion object : Failure.Key
		}

		class IncorrectCredentials : CustomFailure(Companion, "The provided credentials are incorrect"),
			LogIn,
			TokenVerification {
			companion object : Failure.Key
		}
	}
}
