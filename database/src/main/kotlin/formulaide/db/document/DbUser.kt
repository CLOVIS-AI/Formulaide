package formulaide.db.document

import formulaide.api.users.User
import kotlinx.serialization.Serializable

/**
 * Database class that corresponds to [User].
 */
@Serializable
data class DbUser(
	val id: String,
	val email: String,
	val hashedPassword: String,
	val fullName: String,
)

/**
 * Converts a database [DbUser] to a [User].
 */
fun DbUser.toApi() = User(email, fullName)
