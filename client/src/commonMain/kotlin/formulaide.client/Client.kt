package formulaide.client

import formulaide.client.Client.Anonymous
import formulaide.client.Client.Authenticated
import io.ktor.client.*
import io.ktor.client.features.auth.*
import io.ktor.client.features.auth.providers.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.features.json.serializer.KotlinxSerializer.Companion.DefaultJson
import io.ktor.client.request.*
import io.ktor.http.*

/**
 * Common behavior between [Anonymous] and [Authenticated].
 */
sealed class Client(
	val hostUrl: String,
	internal val client: HttpClient
) {

	/**
	 * Makes an HTTP request to the server.
	 */
	internal suspend inline fun <reified Out> request(
		method: HttpMethod,
		url: String,
		body: Any? = null,
		block: HttpRequestBuilder.() -> Unit = {}
	): Out {
		return client.request(hostUrl + url) {
			this.method = method

			if (body != null) {
				contentType(ContentType.Application.Json)
				this.body = body
			}

			block()
		}
	}

	internal suspend inline fun <reified Out> post(
		url: String,
		body: Any? = null,
		block: HttpRequestBuilder.() -> Unit = {}
	) = request<Out>(HttpMethod.Post, url, body, block)

	internal suspend inline fun <reified Out> get(
		url: String,
		body: Any? = null,
		block: HttpRequestBuilder.() -> Unit = {}
	) = request<Out>(HttpMethod.Get, url, body, block)

	companion object {

		internal val jsonSerializer = DefaultJson

		private fun createClient(token: String? = null) = HttpClient {
			install(JsonFeature) {
				serializer = KotlinxSerializer(jsonSerializer)
			}

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

		fun authenticate(token: String) = Authenticated.connect(hostUrl, token)
	}

	class Authenticated private constructor(client: HttpClient, hostUrl: String) : Client(hostUrl, client) {

		/**
		 * Disconnects this client.
		 *
		 * Calling this method is important because it will clear the cookies set by the login.
		 */
		suspend fun logout() = post<String>("/users/logout")

		companion object {
			fun connect(hostUrl: String, token: String) =
				Authenticated(createClient(token), hostUrl)
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
