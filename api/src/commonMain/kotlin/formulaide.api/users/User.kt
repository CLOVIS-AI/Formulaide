package formulaide.api.users

import kotlinx.serialization.Serializable

/**
 * Represents a user.
 *
 * @see NewUser
 * @see PasswordLogin
 */
@Serializable
data class User(
	val email: String,
	val fullName: String,
	val service: ServiceId,
	val administrator: Boolean,
)
