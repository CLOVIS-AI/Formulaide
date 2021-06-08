package formulaide.db.document

import formulaide.api.users.User
import formulaide.db.Database
import kotlinx.serialization.Serializable
import org.litote.kmongo.eq

typealias DbUserId = String

/**
 * Database class that corresponds to [User].
 */
@Serializable
data class DbUser(
	val id: DbUserId,
	val email: String,
	val hashedPassword: String,
	val fullName: String,
	val service: DbServiceId,
	val isAdministrator: Boolean
)

/**
 * Converts a database [DbUser] to a [User].
 */
fun DbUser.toApi() = User(email, fullName)

/**
 * Finds a user in the database, by searching for an exact match with its [email].
 */
suspend fun Database.findUser(email: String): DbUser? =
	users.findOne(DbUser::email eq email)

/**
 * Creates a [user], and returns it.
 */
suspend fun Database.createUser(user: DbUser): DbUser {
	checkNotNull(findService(user.service))

	users.insertOne(user)

	return user
}
