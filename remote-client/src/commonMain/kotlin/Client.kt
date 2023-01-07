package opensavvy.formulaide.remote.client

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.flow.Flow
import opensavvy.formulaide.core.Auth
import opensavvy.formulaide.remote.ApiJson

interface Client {

	val http: HttpClient

	val auth: Flow<Auth>

	companion object {
		fun HttpClientConfig<*>.configureClient() {
			install(ContentNegotiation) {
				json(ApiJson)
			}
		}
	}
}
