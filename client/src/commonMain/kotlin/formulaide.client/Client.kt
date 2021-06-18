package formulaide.client

import formulaide.client.Client.Anonymous
import formulaide.client.Client.Authenticated
import io.ktor.client.*
import io.ktor.client.features.auth.*
import io.ktor.client.features.auth.providers.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.features.logging.*
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

		private fun createClient(token: String? = null) = HttpClient {
			install(JsonFeature) {
				serializer = KotlinxSerializer()
			}

			install(Logging) {
				level = LogLevel.ALL
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
		companion object {
			fun connect(hostUrl: String, token: String) = Authenticated(createClient(token), hostUrl)
		}
	}
}