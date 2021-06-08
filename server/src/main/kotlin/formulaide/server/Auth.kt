package formulaide.server

import at.favre.lib.crypto.bcrypt.BCrypt
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import formulaide.api.users.NewUser
import formulaide.api.users.PasswordLogin
import formulaide.db.Database
import formulaide.db.document.DbUser
import formulaide.db.document.DbUserId
import formulaide.db.document.createUser
import formulaide.db.document.findUser
import org.slf4j.LoggerFactory
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
		val hashedPassword = hash(newUser.password)

		val id = UUID.randomUUID().toString()

		val createdUser = database.createUser(
			DbUser(
				id,
				newUser.user.email,
				hashedPassword,
				newUser.user.fullName,
				newUser.user.service,
				newUser.user.administrator
			)
		)

		return signAccessToken(id) to createdUser
	}

	/**
	 * Checks the validity of a connection based on a hashed password.
	 * @return A valid token if the password was valid.
	 */
	suspend fun login(login: PasswordLogin): Pair<String, DbUser> {
		val user = database.findUser(login.email)
		checkNotNull(user) { "Aucun utilisateur n'a été trouvé avec cette adresse mail" }

		val passwordIsVerified = checkHash(
			login.password,
			user.hashedPassword
		)
		check(passwordIsVerified) { "Le mot de passe donné ne correspond pas à celui stocké" }

		return signAccessToken(user.email) to user
	}

	/**
	 * Checks the validity of a given [token].
	 */
	fun checkToken(
		token: String
	): DbUserId? {
		val accessToken: DecodedJWT
		try {
			accessToken = verifier.verify(JWT.decode(token))
		} catch (e: Exception) {
			logger.warn("Token is invalid: $e")
			return null
		}

		val userId: String? = accessToken.getClaim("userId").asString()
		checkNotNull(userId) { "Le token ne contient pas de 'userId'" }

		return userId
	}

	private fun signAccessToken(email: String): String = JWT.create()
		.withIssuer("formulaide")
		.withClaim("userId", email)
		.sign(algorithm)

	companion object {
		private val logger = LoggerFactory.getLogger(Auth::class.java)

		internal fun hash(clearText: String): String {
			return BCrypt.withDefaults()
				.hashToString(12, clearText.toCharArray())
		}

		internal fun checkHash(clearText: String, hash: String): Boolean {
			return BCrypt.verifyer()
				.verify(
					clearText.toCharArray(),
					hash.toCharArray()
				).verified
		}
	}
}
