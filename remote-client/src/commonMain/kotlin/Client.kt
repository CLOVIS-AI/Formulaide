package opensavvy.formulaide.remote.client

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import opensavvy.formulaide.remote.ApiJson

class Client(
	internal val http: HttpClient,
) {

	companion object {
		fun HttpClientConfig<*>.configureClient() {
			install(ContentNegotiation) {
				json(ApiJson)
			}
		}
	}
}
