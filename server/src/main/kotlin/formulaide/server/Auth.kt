package formulaide.server

import arrow.core.Either
import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.computations.either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import at.favre.lib.crypto.bcrypt.BCrypt
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import formulaide.api.users.NewUser
import formulaide.api.users.PasswordLogin
import formulaide.db.Database
import formulaide.db.createUser
import formulaide.db.document.DbUser
import formulaide.db.findUser
import formulaide.server.DatabaseFailure.Companion.asServerFailure
import java.util.*

/**
 * Handles the authentication of users, with a [JWT] using Bcrypt.
 */
class Auth(private val database: Database) {

	private val secretKey = "some secret key here" //TODO
	private val algorithm = Algorithm.HMAC256(secretKey)
	private val verifier = JWT.require(algorithm).build()

	/**
	 * Creates a new account.
	 * @return A pair of a JWT token and the created user, or [Left] is something went wrong.
	 */
	suspend fun newAccount(newUser: NewUser): Either<Failure, Pair<String, DbUser>> = either {
		val hashedPassword = BCrypt.withDefaults()
			.hash(10, newUser.password.toByteArray(Charsets.UTF_8))

		val id = UUID.randomUUID().toString()
		val emailUser = database.findUser(newUser.user.email).asServerFailure().bind()

		check(
			emailUser == null,
			ifFalse = UnknownFailure("Un utilisateur avec l'adresse '${emailUser!!.email}' existe déjà")
		).bind()

		val createdUser = database.createUser(DbUser(id, newUser.user.email, hashedPassword.toString(), newUser.user.fullName))
			.asServerFailure().bind()

		signAccessToken(id) to createdUser
	}

	/**
	 * Checks the validity of a connection based on a hashed password.
	 * @return A valid token if the password was valid, a [Failure] otherwise.
	 */
	suspend fun login(login: PasswordLogin): Either<Failure, String> {
		return either {
			val user = database.findUser(login.email)
				.asServerFailure()
				.collapseNullable(left = UnknownFailure("Aucun utilisateur n'a été trouvé avec cette adresse mail"))
				.bind()

			BCrypt.verifyer()
				.verify(
					login.password.toByteArray(Charsets.UTF_8),
					user.hashedPassword.toByteArray(Charsets.UTF_8)
				).verified
				.rightIfTrue(ifFalse = InvalidToken).bind()

			signAccessToken(user.email).right().bind()
		}
	}

	/**
	 * Checks the validity of a given [token].
	 */
	suspend fun checkToken(
		token: String
	): Either<Failure, String> = either {
		val accessToken = Either.catch { verifier.verify(JWT.decode(token)) }
			.mapLeft { InvalidToken }.bind()

		val userId: String? = accessToken.getClaim("userId").asString()

		Either.fromNullable(userId)
			.mapLeft { InvalidToken }
			.bind()
	}

	private fun signAccessToken(email: String): String = JWT.create()
		.withIssuer("formulaide")
		.withClaim("userId", email)
		.sign(algorithm)

	private fun <T> Boolean.rightIfTrue(ifFalse: T) =
		if (this) Right(true)
		else Left(ifFalse)

	private fun <L, R : Any> Either<L, R?>.collapseNullable(left: L): Either<L, R> =
		flatMap {
			it?.right() ?: left.left()
		}

	private fun <L> check(condition: Boolean, ifFalse: L) =
		if (condition) Right(Unit)
		else Left(ifFalse)
}
