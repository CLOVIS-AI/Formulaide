package formulaide.db.document

import formulaide.api.types.Email
import formulaide.api.types.Ref
import formulaide.api.users.User
import formulaide.db.Database
import kotlinx.serialization.Serializable
import org.litote.kmongo.eq
import org.litote.kmongo.ne

typealias DbUserId = String

/**
 * Database class that corresponds to [User].
 *
 * @property tokenVersion An integer
 */
@Serializable
data class DbUser(
	val id: DbUserId,
	val email: String,
	val hashedPassword: String,
	val fullName: String,
	val service: DbServiceId,
	val isAdministrator: Boolean,
	val enabled: Boolean? = true,
	val tokenVersion: ULong = 0u,
)

/**
 * Converts a database [DbUser] to a [User].
 */
fun DbUser.toApi() = User(
	Email(email), fullName, Ref(service.toString()), isAdministrator, enabled ?: false)

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

suspend fun Database.listEnabledUsers(): List<DbUser> =
	users.find(DbUser::enabled ne false).toList()

suspend fun Database.listAllUsers(): List<DbUser> =
	users.find().toList()

/**
 * Edits a [user].
 *
 * All optional arguments represent modification requests. `null` means that no modification is requested.
 *
 * At least one argument must be non-`null`.
 */
suspend fun Database.editUser(
	user: DbUser,
	newEnabled: Boolean? = null,
	newIsAdministrator: Boolean? = null,
): DbUser {
	var newUser = user

	if (newEnabled != null)
		newUser = newUser.copy(enabled = newEnabled)

	if (newIsAdministrator != null)
		newUser = newUser.copy(isAdministrator = newIsAdministrator)

	require(user != newUser) { "La demande de modification de l'utilisateur ${user.email} n'apporte aucune modification" }

	users.updateOne(DbUser::id eq user.id, newUser)
	return newUser
}

/**
 * Replaces the [user]'s password.
 *
 * This function does not do any security check. It is the caller's responsibility to check whether the password should, in fact, be replaced.
 */
suspend fun Database.editUserPassword(user: DbUser, newHashedPassword: String): DbUser {
	val newUser = user.copy(
		hashedPassword = newHashedPassword,
		tokenVersion = user.tokenVersion + 1u,
	)

	users.updateOne(DbUser::id eq newUser.id, newUser)
	return newUser
}
