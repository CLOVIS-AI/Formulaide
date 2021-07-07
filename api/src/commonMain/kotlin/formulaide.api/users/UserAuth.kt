package formulaide.api.users

import formulaide.api.types.Email
import kotlinx.serialization.Serializable

/**
 * Data required to create a new user.
 *
 * @property password The password that will be used for this [User] (will be hashed by the server)
 */
@Serializable
data class NewUser(
	val password: String,
	val user: User
)

/**
 * Data required to connect as a user.
 *
 * @property password The user's password (will be hashed by the server)
 * @see User
 */
@Serializable
data class PasswordLogin(
	val password: String,
	val email: String,
)

/**
 * Server response for valid tokens.
 * @see PasswordLogin
 */
@Serializable
data class TokenResponse(
	val token: String,
)

/**
 * Data required to edit a [user].
 *
 * All fields (but [user]) represent the request to edit the corresponding field in [User].
 * A value of `null` means that no modification is requested.
 * At least one modification should be requested.
 */
@Serializable
data class UserEdits(
	val user: Email,
	val enabled: Boolean? = null,
	val administrator: Boolean? = null,
)

/**
 * Data required to edit a [user]'s password.
 *
 * @property oldPassword The currently-used password. The administrator doesn't have to provide this.
 */
@Serializable
data class PasswordEdit(
	val user: Email,
	val oldPassword: String?,
	val newPassword: String,
)
