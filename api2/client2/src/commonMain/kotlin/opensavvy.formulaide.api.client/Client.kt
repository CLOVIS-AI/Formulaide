package opensavvy.formulaide.api.client

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.emitAll
import opensavvy.backbone.defaultRefCache
import opensavvy.cache.ExpirationCache.Companion.expireAfter
import opensavvy.cache.MemoryCache.Companion.cachedInMemory
import opensavvy.formulaide.api.Context
import opensavvy.formulaide.api.api2
import opensavvy.formulaide.core.Department
import opensavvy.formulaide.core.User
import opensavvy.spine.Parameters
import opensavvy.spine.ktor.client.request
import opensavvy.state.State
import opensavvy.state.state
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.minutes

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

	internal var context = Context(null, User.Role.ANONYMOUS)

	val role = this.context.role
	val user = this.context.user

	val departments = Departments(
		this,
		defaultRefCache<Department>()
			.cachedInMemory(context)
	)

	val users = Users(
		this,
		defaultRefCache<User>()
			.cachedInMemory(context)
			.expireAfter(10.minutes, context)
	)

	fun ping(): State<Unit> = state {
		val result = http.request(api2.ping.get, api2.ping.idOf(), Unit, Parameters.Empty, context)
		emitAll(result)
	}
}
