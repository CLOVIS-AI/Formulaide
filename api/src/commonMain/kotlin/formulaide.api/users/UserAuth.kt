package formulaide.api.users

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
	val email: String
)
