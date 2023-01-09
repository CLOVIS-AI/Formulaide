package opensavvy.formulaide.core

import opensavvy.backbone.Backbone
import opensavvy.formulaide.core.data.Email
import opensavvy.formulaide.core.data.Password
import opensavvy.formulaide.core.data.Token
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
		Anonymous,
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
			val User?.role get() = this?.role ?: Anonymous
		}
	}

	data class Ref(
		val id: String,
		override val backbone: Service,
	) : opensavvy.backbone.Ref<User> {

		/**
		 * Joins [department].
		 *
		 * @see User.departments
		 * @see Service.join
		 */
		suspend fun join(department: Department.Ref) = backbone.join(this, department)

		/**
		 * Leaves [department].
		 *
		 * @see User.departments
		 * @see Service.leave
		 */
		suspend fun leave(department: Department.Ref) = backbone.leave(this, department)

		/**
		 * Enables this user.
		 *
		 * @see User.active
		 * @see Service.edit
		 */
		suspend fun enable() = backbone.edit(this, active = true)

		/**
		 * Disables this user.
		 *
		 * @see User.active
		 * @see Service.edit
		 */
		suspend fun disable() = backbone.edit(this, active = false)

		/**
		 * Promotes this user to [Role.Administrator].
		 *
		 * @see User.administrator
		 * @see Service.edit
		 */
		suspend fun promote() = backbone.edit(this, administrator = true)

		/**
		 * Demotes this user to [Role.Employee].
		 *
		 * @see User.administrator
		 * @see Service.edit
		 */
		suspend fun demote() = backbone.edit(this, administrator = false)

		/**
		 * Resets this user's password.
		 *
		 * @see User.singleUsePassword
		 * @see Service.resetPassword
		 */
		suspend fun resetPassword() = backbone.resetPassword(this)

		/**
		 * Sets this user's password.
		 *
		 * @see User.singleUsePassword
		 * @see Service.setPassword
		 */
		suspend fun setPassword(oldPassword: String, newPassword: Password) =
			backbone.setPassword(this, oldPassword, newPassword)

		/**
		 * Verifies this [token].
		 *
		 * @see Service.verifyToken
		 */
		suspend fun verifyToken(token: Token) = backbone.verifyToken(this, token)

		/**
		 * Invalidates this [token].
		 */
		suspend fun logOut(token: Token) = backbone.logOut(this, token)

		override fun toString() = "Utilisateur $id"
	}

	interface Service : Backbone<User> {

		/**
		 * Lists all users.
		 *
		 * Requires administrator authentication.
		 */
		suspend fun list(includeClosed: Boolean = false): Outcome<List<Ref>>

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
		): Outcome<Pair<Ref, Password>>

		/**
		 * Adds [department] to the list of departments [user] is a part of.
		 *
		 * Requires administrator authentication.
		 */
		suspend fun join(
			user: Ref,
			department: Department.Ref,
		): Outcome<Unit>

		/**
		 * Removes [department] from the list of departments [user] is a part of.
		 *
		 * Requires administrator authentication.
		 */
		suspend fun leave(
			user: Ref,
			department: Department.Ref,
		): Outcome<Unit>

		/**
		 * Edits [user].
		 *
		 * Requires administrator authentication.
		 */
		suspend fun edit(
			user: Ref,
			active: Boolean? = null,
			administrator: Boolean? = null,
		): Outcome<Unit>

		//region Account management

		/**
		 * Resets [user]'s password.
		 *
		 * The user will be disconnected from their account on all their devices.
		 *
		 * Requires administrator authentication.
		 *
		 * @return A new single-use password.
		 */
		suspend fun resetPassword(
			user: Ref,
		): Outcome<Password>

		/**
		 * Sets [user]'s password.
		 *
		 * A user may only set their own password.
		 */
		suspend fun setPassword(
			user: Ref,
			oldPassword: String,
			newPassword: Password,
		): Outcome<Unit>

		//endregion
		//region Authentication

		/**
		 * Checks that [token] is a valid token for [user].
		 */
		suspend fun verifyToken(
			user: Ref,
			token: Token,
		): Outcome<Unit>

		/**
		 * Checks that [email] and [password] match an existing user.
		 *
		 * @return The reference to the logged-in user, and a valid connection token.
		 */
		suspend fun logIn(
			email: Email,
			password: Password,
		): Outcome<Pair<Ref, Token>>

		/**
		 * Invalidates [token] for [user].
		 */
		suspend fun logOut(
			user: Ref,
			token: Token,
		): Outcome<Unit>

		//endregion
	}
}
