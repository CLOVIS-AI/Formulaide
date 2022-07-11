package formulaide.core

import opensavvy.backbone.Backbone
import opensavvy.backbone.Ref

/**
 * An account in the Formulaide tool.
 *
 * @property open It is not possible to log into a closed user.
 */
data class User(
	val email: String,
	val fullName: String,
	val departments: Set<Ref<Department>>,
	val administrator: Boolean,
	val open: Boolean,
)

interface UserBackbone : Backbone<User> {

	/**
	 * Lists all users.
	 *
	 * Requires administrator authentication.
	 */
	suspend fun all(includeClosed: Boolean = false): List<Ref<User>>

	/**
	 * Finds information about the currently-logged-in user.
	 *
	 * Requires employee authentication.
	 */
	suspend fun me(): User

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
		departments: Set<Ref<Department>>,
		administrator: Boolean,
		password: String,
	)

	/**
	 * Edits a [user].
	 *
	 * For all parameters, `null` means "no change", any other value represents a request to replace the current value by that one.
	 */
	suspend fun edit(
		user: Ref<User>,
		open: Boolean? = null,
		administrator: Boolean? = null,
		departments: Set<Ref<Department>>? = null,
	): User

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
		user: Ref<User>,
		oldPassword: String?,
		newPassword: String,
	)
}
