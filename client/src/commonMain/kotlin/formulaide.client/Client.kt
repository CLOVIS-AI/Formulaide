package formulaide.client

import io.ktor.client.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import io.ktor.http.*

class Client(private val hostUrl: String) {

	internal val client = HttpClient {
		install(JsonFeature) {
			serializer = KotlinxSerializer()
		}
	}

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

}
