package formulaide.server

import at.favre.lib.crypto.bcrypt.BCrypt
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import com.auth0.jwt.interfaces.Payload
import formulaide.api.types.Email
import formulaide.api.users.NewUser
import formulaide.api.users.PasswordLogin
import formulaide.db.Database
import formulaide.db.document.DbUser
import formulaide.db.document.createUser
import formulaide.db.document.findUser
import io.ktor.application.*
import io.ktor.auth.*
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.*

/**
 * Handles the authentication of users, with a [JWT] using Bcrypt.
 */
class Auth(private val database: Database) {

	private val secretKey = System.getenv("formulaide_jwt_secret")
		?: error("La variable d'environnement 'formulaide_jwt_secret' n'est pas paramétrée")
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

		return signAccessToken(newUser.user.email, newUser.user.administrator) to createdUser
	}

	/**
	 * Checks the validity of a connection based on a hashed password.
	 * @return A triple of an access token, a refresh token, and their matching user.
	 */
	suspend fun login(login: PasswordLogin): Triple<String, String, DbUser> {
		val user = database.findUser(login.email)
		checkNotNull(user) { "Aucun utilisateur n'a été trouvé avec cette adresse mail" }
		check(user.enabled == true) { "Cet utilisateur n'est pas activé" }

		val passwordIsVerified = checkHash(
			login.password,
			user.hashedPassword
		)
		check(passwordIsVerified) { "Le mot de passe donné ne correspond pas à celui stocké" }

		return Triple(
			signAccessToken(Email(user.email), user.isAdministrator),
			signRefreshToken(Email(user.email), user.tokenVersion),
			user
		)
	}

	/**
	 * Checks the validity of a connection based on a refresh token.
	 *
	 * @return a pair of an access token and its matching user.
	 */
	suspend fun loginWithRefreshToken(refreshToken: String): Pair<String, DbUser> {
		val user = checkRefreshTokenJWT(verifier.verify(JWT.decode(refreshToken)))

		return Pair(
			signAccessToken(Email(user.email), user.isAdministrator),
			user
		)
	}

	/**
	 * Checks the validity of a given [token].
	 */
	fun checkToken(
		token: String,
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

	private fun checkCommonTokenJWT(
		payload: Payload,
	): String {
		val dateNow = Date.from(Instant.now())

		check(payload.issuer == "formulaide") { "Le token est invalide, il ne provient pas de 'formulaide' : ${payload.issuer}" }
		check(payload.expiresAt >= dateNow) { "Le token a expiré à ${payload.expiresAt}" }

		return payload.getClaim("userId").asString()
			?: error("Le token est invalide, il ne contient pas de 'userId'")
	}

	/**
	 * Checks the validity of a given [payload].
	 * @throws IllegalStateException if the token doesn't have the correct fields
	 */
	fun checkTokenJWT(
		payload: Payload,
	): AuthPrincipal {
		val email = checkCommonTokenJWT(payload)

		val type: String? = payload.getClaim("type").asString()
		check(type == "access") { "Le token est invalide parce qu'il est du mauvais type ('access' était attendu) : $type" }

		val isAdmin: Boolean? = payload.getClaim("admin").asBoolean()
		checkNotNull(isAdmin) { "Le token est invalide, il ne contient pas de rôle ('admin') : $isAdmin" }

		return AuthPrincipal(payload, Email(email), isAdmin)
	}

	private suspend fun checkRefreshTokenJWT(
		payload: Payload,
	): DbUser {
		val email = checkCommonTokenJWT(payload)

		val type: String? = payload.getClaim("type").asString()
		check(type == "refresh") { "Le token est invalide parce qu'il est du mauvais type ('refresh' était attendu) : $type" }

		val tokenVersion: ULong? = payload.getClaim("tokVer").asLong()?.toULong()
		checkNotNull(tokenVersion) { "Le token est invalide, il ne contient pas de 'tokVer', ou a une valeur invalide : $tokenVersion" }

		val databaseUser = database.findUser(email)
			?: error("Aucun utilisateur ne correspond à l'identité de ce token, c'est impossible !")

		check(tokenVersion == databaseUser.tokenVersion) { "La version du token ne correspond pas, par exemple parce que le mot de passe a été modifié récemment" }

		return databaseUser
	}

	private fun signAccessToken(email: Email, admin: Boolean): String =
		JWT.create()
			.withIssuer("formulaide")
			.withClaim("userId", email.email)
			.withClaim("admin", admin)
			.withClaim("type", "access")
			.withExpiresAt(Date.from(Instant.now() + accessTokenExpiration))
			.sign(algorithm)

	private fun signRefreshToken(email: Email, tokenVersion: ULong): String =
		JWT.create()
			.withIssuer("formulaide")
			.withClaim("userId", email.email)
			.withClaim("tokVer", tokenVersion.toLong())
			.withClaim("type", "refresh")
			.withExpiresAt(Date.from(Instant.now() + refreshTokenExpiration))
			.sign(algorithm)

	/**
	 * The [Principal] that represents that a user has the right to access the employee-only section of Formulaide.
	 * @property payload The JWT token that proves the legality of the access
	 * @property email The email of the user accessing the data
	 * @property isAdmin Whether this user has the 'admin' right or not
	 */
	data class AuthPrincipal(val payload: Payload, val email: Email, val isAdmin: Boolean) :
		Principal

	companion object {
		private val logger = LoggerFactory.getLogger(Auth::class.java)

		val accessTokenExpiration: Duration = Duration.ofMinutes(30)
		val refreshTokenExpiration: Duration = Duration.ofDays(7)

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

			val user = database.findUser(principal.email.email)
			requireNotNull(user) { "Aucun utilisateur ne correspond à ce token" }
			require(user.enabled == true) { "Cet utilisateur a été désactivé, le token est donc invalide" }

			return user
		}

		suspend fun ApplicationCall.requireAdmin(database: Database): DbUser {
			val employee = requireEmployee(database)
			require(employee.isAdministrator) { "L'utilisateur n'a pas les droits d'administration" }
			return employee
		}
	}
}
