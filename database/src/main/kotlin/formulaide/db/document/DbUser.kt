package formulaide.db.document

import formulaide.api.types.Email
import formulaide.api.types.Ref
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
fun DbUser.toApi() = User(
	Email(email), fullName, Ref(service.toString()), isAdministrator)

/**
 * Finds a user in the database, by searching for an exact match with its [email].
 */
suspend fun Database.findUser(email: String): DbUser? =
	users.findOne(DbUser::email eq email)

/**
 * Finds a user in the database, by searching for an exact match with its [id].
 */
suspend fun Database.findUserById(id: DbUserId): DbUser? =
	users.findOne(DbUser::id eq id)

/**
 * Creates a [user], and returns it.
 */
suspend fun Database.createUser(user: DbUser): DbUser {
	checkNotNull(findService(user.service)) { "Le service ${user.service} n'existe pas" }
	check(findUser(user.email) == null) { "Un utilisateur avec cette adresse mail existe déjà" }
	check(findUserById(user.id) == null) { "Un utilisateur avec cet identifiant existe déjà" }

	users.insertOne(user)

	return user
}
