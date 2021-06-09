package formulaide.client

import io.ktor.client.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.features.logging.*
import io.ktor.client.request.*
import io.ktor.http.*

/**
 * Common behavior between [AnonymousClient] and [AnonymousClient].
 */
sealed class Client(internal val hostUrl: String) {

	internal abstract val client: HttpClient
	internal open val config: HttpRequestBuilder.() -> Unit = {}

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
			config()

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

}

/**
 * A client without credentials.
 */
class AnonymousClient(hostUrl: String) : Client(hostUrl) {

	override val client = HttpClient {
		install(JsonFeature) {
			serializer = KotlinxSerializer()
		}

		install(Logging)
	}

}

/**
 * A client connected by a [token].
 * To get a token, use [formulaide.client.routes.login].
 */
class AuthenticatedClient(private val _client: Client, private val token: String) : Client(_client.hostUrl) {

	override val client: HttpClient
		get() = _client.client

	//TODO: authenticate

}
