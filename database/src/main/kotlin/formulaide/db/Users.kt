package formulaide.db

import arrow.core.Either
import arrow.core.right
import formulaide.db.document.DbUser
import org.litote.kmongo.eq

/**
 * Finds a user in the database, by searching for an exact match with its [email].
 */
suspend fun Database.findUser(email: String): Either<Failure, DbUser?> {
	val findOne = users.findOne(DbUser::email eq email)

	return findOne.right()
}

suspend fun Database.createUser(user: DbUser): Either<Failure, DbUser> {
	users.insertOne(user)

	return user.right()
}
