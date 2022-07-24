package formulaide.core

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import opensavvy.backbone.Backbone

/**
 * An account in the Formulaide tool.
 *
 * @property open It is not possible to log into a closed user.
 */
@Serializable
data class User(
	val email: String,
	val fullName: String,
	val departments: Set<@Contextual Department.Ref>,
	val administrator: Boolean,
	val open: Boolean,
) {

	data class Ref(val email: String, override val backbone: UserBackbone) : opensavvy.backbone.Ref<User> {
		override fun toString() = "User $email"
	}
}

interface UserBackbone : Backbone<User> {

	/**
	 * Lists all users.
	 *
	 * Requires administrator authentication.
	 */
	suspend fun all(includeClosed: Boolean = false): List<User.Ref>

	/**
	 * Finds information about the currently-logged-in user.
	 *
	 * Requires employee authentication.
	 */
	suspend fun me(): User.Ref

	/**
	 * Logs in.
	 *
	 * @return a refresh token.
	 */
	suspend fun logIn(email: String, password: String): String

	/**
	 * Creates a new user.
	 *
	 * Requires administrator authentication.
	 */
	suspend fun create(
		email: String,
		fullName: String,
		departments: Set<Department.Ref>,
		administrator: Boolean,
		password: String,
	): User.Ref

	/**
	 * Edits a [user].
	 *
	 * For all parameters, `null` means "no change", any other value represents a request to replace the current value by that one.
	 */
	suspend fun edit(
		user: User.Ref,
		open: Boolean? = null,
		administrator: Boolean? = null,
		departments: Set<Department.Ref>? = null,
	)

	/**
	 * Sets the [user]'s password.
	 *
	 * Requires employee authentication to edit your own account.
	 * In that case, [oldPassword] is mandatory.
	 *
	 * Requires administrator authentication to edit any other account.
	 * In that case, [oldPassword] is optional.
	 */
	suspend fun setPassword(
		user: User.Ref,
		oldPassword: String?,
		newPassword: String,
	)
}
