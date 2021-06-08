package formulaide.server

import at.favre.lib.crypto.bcrypt.BCrypt
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import formulaide.api.users.NewUser
import formulaide.api.users.PasswordLogin
import formulaide.db.Database
import formulaide.db.document.DbUser
import formulaide.db.document.createUser
import formulaide.db.document.findUser
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
	 * @return A pair of a JWT token and the created user.
	 */
	suspend fun newAccount(newUser: NewUser): Pair<String, DbUser> {
		val hashedPassword = BCrypt.withDefaults()
			.hash(10, newUser.password.toByteArray(Charsets.UTF_8))

		val id = UUID.randomUUID().toString()

		val createdUser = database.createUser(
			DbUser(
				id,
				newUser.user.email,
				hashedPassword.toString(),
				newUser.user.fullName,
				newUser.service,
				newUser.administrator
			)
		)

		return signAccessToken(id) to createdUser
	}

	/**
	 * Checks the validity of a connection based on a hashed password.
	 * @return A valid token if the password was valid.
	 */
	suspend fun login(login: PasswordLogin): String {
		val user = database.findUser(login.email)
		checkNotNull(user) { "Aucun utilisateur n'a été trouvé avec cette adresse mail" }

		val tokenIsVerified = BCrypt.verifyer()
			.verify(
				login.password.toByteArray(Charsets.UTF_8),
				user.hashedPassword.toByteArray(Charsets.UTF_8)
			).verified
		check(tokenIsVerified) { "Le token est invalide" }

		return signAccessToken(user.email)
	}

	/**
	 * Checks the validity of a given [token].
	 */
	fun checkToken(
		token: String
	): String {
		val accessToken = verifier.verify(JWT.decode(token))

		val userId: String? = accessToken.getClaim("userId").asString()
		checkNotNull(userId) { "Le token ne contient pas de 'userId'" }

		return userId
	}

	private fun signAccessToken(email: String): String = JWT.create()
		.withIssuer("formulaide")
		.withClaim("userId", email)
		.sign(algorithm)
}
