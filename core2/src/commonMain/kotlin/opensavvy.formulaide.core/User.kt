package opensavvy.formulaide.core

import opensavvy.backbone.Backbone
import opensavvy.state.slice.Slice

data class User(
	val email: String,
	val name: String,
	val open: Boolean,
	val departments: Set<Department.Ref>,
	val administrator: Boolean,

	/** If `true`, this account is currently authenticated by a single-use password. */
	val forceResetPassword: Boolean,
) {

	val role
		get() = when (administrator) {
			true -> Role.ADMINISTRATOR
			false -> Role.EMPLOYEE
		}

	data class Ref(val id: String, override val backbone: AbstractUsers) : opensavvy.backbone.Ref<User> {
		override fun toString() = "Utilisateur $id"
	}

	enum class Role {
		ANONYMOUS,
		EMPLOYEE,
		ADMINISTRATOR,
		;

		companion object {
			val User?.role get() = this?.role ?: EMPLOYEE
		}
	}
}

interface AbstractUsers : Backbone<User> {

	/**
	 * Lists all users.
	 *
	 * Requires administrator authorization.
	 */
	suspend fun list(includeClosed: Boolean = false): Slice<List<User.Ref>>

	/**
	 * Creates a new user.
	 *
	 * Requires administrator authorization.
	 *
	 * @return Reference to the created user and the generated single-use password.
	 */
	suspend fun create(
		email: String,
		fullName: String,
		departments: Set<Department.Ref>,
		administrator: Boolean,
	): Slice<Pair<User.Ref, String>>

	/**
	 * Edits [user].
	 *
	 * For all parameters, `null` means "no change".
	 *
	 * Requires administrator authorization.
	 */
	suspend fun edit(
		user: User.Ref,
		open: Boolean? = null,
		administrator: Boolean? = null,
		departments: Set<Department.Ref>? = null,
	): Slice<Unit>

	/**
	 * Resets [user]'s password.
	 *
	 * The user will automatically be disconnected from their account on all their devices.
	 *
	 * @return A new single-use password.
	 */
	suspend fun resetPassword(user: User.Ref): Slice<String>

}
