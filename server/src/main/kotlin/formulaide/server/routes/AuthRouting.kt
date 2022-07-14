package formulaide.server.routes

import formulaide.api.bones.ApiPasswordLogin
import formulaide.server.Auth
import formulaide.server.allowUnsafeCookie
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.date.*

/**
 * The authentication endpoint: `/api/auth`.
 */
@Suppress("MemberVisibilityCanBePrivate")
object AuthRouting {
	private lateinit var auth: Auth

	internal fun Routing.enable(auth: Auth) {
		this@AuthRouting.auth = auth

		route("/api/auth") {
			logIn()
			logOut()
			refresh()
		}
	}

	private fun ApplicationCall.setRefreshTokenCookie(value: String) {
		val extensions = HashMap<String, String?>()
		extensions["SameSite"] = "Strict"

		if (!this.application.developmentMode && !allowUnsafeCookie)
			extensions["Secure"] = null

		response.cookies.append(
			Cookie(
				"REFRESH-TOKEN",
				value = value,
				expires = GMTDate() + Auth.refreshTokenExpiration.toMillis(),
				httpOnly = true,
				path = "/",
				extensions = extensions,
			)
		)
	}

	/**
	 * The user log-in endpoint: `/api/auth/login`.
	 *
	 * ### Post
	 *
	 * Logs in.
	 *
	 * - Body: [ApiPasswordLogin]
	 * - Response: an access token ([String])
	 *
	 * The server also sets the `REFRESH-TOKEN` cookie (HTTP-only).
	 */
	fun Route.logIn() {
		post("/login") {
			val login = call.receive<ApiPasswordLogin>()

			try {
				val (accessToken, refreshToken, _) = auth.login(login)

				call.setRefreshTokenCookie(refreshToken)
				call.respond(accessToken)
			} catch (e: Exception) {
				// Explain why the user was refused in the logs,
				// but do not send that information to the user (in case they're an attacker).
				e.printStackTrace()

				call.respondText(
					"Les informations de connexion sont incorrectes. Veuillez attendre quelques secondes avant de réessayer.",
					status = HttpStatusCode.Forbidden
				)
			}
		}
	}

	/**
	 * The user log-out endpoint: `/api/auth/logout`.
	 *
	 * ### Post
	 *
	 * Logs out.
	 *
	 * - Does not require any authentication.
	 * - Response: `"Success"`
	 *
	 * The server removes the `REFRESH-TOKEN` cookie.
	 */
	fun Route.logOut() {
		post("/logout") {
			call.setRefreshTokenCookie("NOT CONNECTED")
			call.respond("Success")
		}
	}

	/**
	 * The token refresh endpoint: `/api/auth/refreshToken`.
	 *
	 * ### Post
	 *
	 * Uses the refresh token to request a new access token.
	 *
	 * - Requires a valid refresh token
	 * - Response: a new access token ([String])
	 */
	fun Route.refresh() {
		post("/refreshToken") {
			val refreshToken = call.request.cookies["REFRESH-TOKEN"]
				?: error("Impossible de demander un nouvel token d'accès sans fournir de token de réactualisation")

			val (accessToken, _) = auth.loginWithRefreshToken(refreshToken)

			call.setRefreshTokenCookie(refreshToken)
			call.respond(accessToken)
		}
	}
}
