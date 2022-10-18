package opensavvy.formulaide.api.client

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.SupervisorJob
import opensavvy.backbone.defaultRefCache
import opensavvy.cache.MemoryCache.Companion.cachedInMemory
import opensavvy.formulaide.api.Context
import opensavvy.formulaide.core.Department
import kotlin.coroutines.CoroutineContext

class Client(
	hostUrl: String,
	context: CoroutineContext = SupervisorJob(),
) {

	internal val http = HttpClient {
		install(ContentNegotiation) {
			json()
		}

		install(DefaultRequest) {
			url(hostUrl)
		}
	}

	internal val context = Context()

	val departments = Departments(
		this,
		defaultRefCache<Department>()
			.cachedInMemory(context)
	)

}
