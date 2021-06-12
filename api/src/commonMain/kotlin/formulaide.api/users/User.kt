package formulaide.api.users

import formulaide.api.types.Email
import kotlinx.serialization.Serializable

/**
 * Represents a user.
 *
 * @see NewUser
 * @see PasswordLogin
 */
@Serializable
data class User(
	val email: Email,
	val fullName: String,
	val service: ServiceId,
	val administrator: Boolean,
)
