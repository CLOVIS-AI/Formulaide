package formulaide.server

import at.favre.lib.crypto.bcrypt.BCrypt
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import com.auth0.jwt.interfaces.Payload
import formulaide.api.users.NewUser
import formulaide.api.users.PasswordLogin
import formulaide.db.Database
import formulaide.db.document.DbUser
import formulaide.db.document.DbUserId
import formulaide.db.document.createUser
import formulaide.db.document.findUser
import io.ktor.application.*
import io.ktor.auth.*
import org.slf4j.LoggerFactory
import java.util.*

/**
 * Handles the authentication of users, with a [JWT] using Bcrypt.
 */
class Auth(private val database: Database) {

	private val secretKey = "some secret key here" //TODO
	private val algorithm = Algorithm.HMAC256(secretKey)
	internal val verifier = JWT
		.require(algorithm)
		.withIssuer("formulaide")
		.build()

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
				newUser.user.email.email,
				hashedPassword,
				newUser.user.fullName,
				newUser.user.service.id.toInt(),
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
	): AuthPrincipal? {
		val accessToken: DecodedJWT
		try {
			accessToken = verifier.verify(JWT.decode(token))
		} catch (e: Exception) {
			logger.warn("Token is invalid: $e")
			return null
		}

		return checkTokenJWT(accessToken)
	}

	/**
	 * Checks the validity of a given [payload].
	 * @throws IllegalStateException if the token doesn't have the correct fields
	 */
	fun checkTokenJWT(
		payload: Payload
	): AuthPrincipal {
		val userId: String? = payload.getClaim("userId").asString()
		checkNotNull(userId) { "Le token ne contient pas de 'userId'" }

		return AuthPrincipal(payload, userId)
	}

	private fun signAccessToken(email: String): String = JWT.create()
		.withIssuer("formulaide")
		.withClaim("userId", email)
		.sign(algorithm)

	/**
	 * The [Principal] that represents that a user has the right to access the employee-only section of Formulaide.
	 * @property payload The JWT token that proves the legality of the access
	 * @property userId The ID of the user accessing the data
	 */
	data class AuthPrincipal(val payload: Payload, val userId: DbUserId) : Principal

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

		const val Employee = "jwt-auth"

		suspend fun ApplicationCall.requireEmployee(database: Database): DbUser {
			val principal = authentication.principal ?: error("Authentification manquante")
			require(principal is AuthPrincipal) { "Authentification invalide" }
			return database.findUser(principal.userId) ?: error("Aucun utilisateur ne correspond à ce token")
		}

		suspend fun ApplicationCall.requireAdmin(database: Database): DbUser {
			val employee = requireEmployee(database)
			require(employee.isAdministrator) { "L'utilisateur n'a pas les droits d'administration" }
			return employee
		}
	}
}
