package formulaide.client

import formulaide.api.users.User
import formulaide.client.Client.Anonymous
import formulaide.client.Client.Authenticated
import formulaide.client.files.MultipartUpload
import formulaide.client.routes.getMe
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*

/**
 * Common behavior between [Anonymous] and [Authenticated].
 */
sealed class Client(
	val hostUrl: String,
	internal val client: HttpClient,
) {

	/**
	 * Makes an HTTP request to the server.
	 */
	internal suspend inline fun <reified Out> request(
		method: HttpMethod,
		url: String,
		body: Any? = null,
		block: HttpRequestBuilder.() -> Unit = {},
	): Out {
		return client.request(hostUrl + url) {
			this.method = method

			if (body != null) {
				contentType(ContentType.Application.Json)
				setBody(body)
			}

			block()
		}.body()
	}

	internal suspend inline fun <reified Out> post(
		url: String,
		body: Any? = null,
		block: HttpRequestBuilder.() -> Unit = {},
	) = request<Out>(HttpMethod.Post, url, body, block)

	internal suspend inline fun <reified Out> get(
		url: String,
		body: Any? = null,
		block: HttpRequestBuilder.() -> Unit = {},
	) = request<Out>(HttpMethod.Get, url, body, block)

	internal suspend inline fun <reified Out> postMultipart(
		url: String,
		vararg parts: MultipartUpload,
		block: HttpRequestBuilder.() -> Unit = {},
	): Out {
		parts.forEach { it.load() }

		return post(url) {
			setBody(
				MultiPartFormDataContent(
					formData {
						for (part in parts)
							part.applyTo(this)
					}
				)
			)
			block()
		}
	}

	companion object {

		internal val jsonSerializer = DefaultJson

		private fun createClient(token: String? = null) = HttpClient {
			install(ContentNegotiation) {
				json(jsonSerializer)
			}

			expectSuccess = true

			if (token != null)
				install(Auth) {
					bearer { //TODO: audit
						loadTokens {
							BearerTokens(accessToken = token, refreshToken = token)
						}

						refreshTokens {
							BearerTokens(accessToken = token, refreshToken = token)
						}
					}
				}
		}

	}

	class Anonymous private constructor(client: HttpClient, hostUrl: String) : Client(hostUrl, client) {
		companion object {
			fun connect(hostUrl: String) = Anonymous(createClient(), hostUrl)
		}

		suspend fun authenticate(token: String) = Authenticated.connect(hostUrl, token)
	}

	class Authenticated private constructor(client: HttpClient, hostUrl: String) : Client(hostUrl, client) {

		/**
		 * The profile of the user we are connected as.
		 */
		lateinit var me: User
			private set

		/**
		 * Disconnects this client.
		 *
		 * Calling this method is important because it will clear the cookies set by the login.
		 */
		suspend fun logout() = post<String>("/users/logout")

		companion object {
			suspend fun connect(hostUrl: String, token: String) =
				Authenticated(createClient(token), hostUrl).apply {
					me = getMe()
				}
		}
	}
}

/**
 * Gets a new **access token** using the currently existing **refresh token** stored in the cookies.
 *
 * This method is not multiplatform because it requires access to the cookies, which Ktor doesn't provide at the moment.
 *
 * @return a valid access token if one could be queried, `null` otherwise
 */
expect suspend fun Client.refreshToken(): String?
