package opensavvy.formulaide.api.client

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import opensavvy.backbone.defaultRefCache
import opensavvy.cache.ExpirationCache.Companion.expireAfter
import opensavvy.cache.MemoryCache.Companion.cachedInMemory
import opensavvy.formulaide.api.Context
import opensavvy.formulaide.api.api2
import opensavvy.formulaide.core.Department
import opensavvy.formulaide.core.Form
import opensavvy.formulaide.core.Template
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

	/**
	 * `true` if this [Client] is connected to the development server.
	 */
	val isDevelopment = "localhost" in hostUrl

	internal var token: String? = null

	internal val http = HttpClient {
		install(ContentNegotiation) {
			json()
		}

		install(DefaultRequest) {
			url(hostUrl)
		}

		install(Auth) {
			bearer {
				refreshTokens {
					// when Ktor receives a 401, it retries with the token
					// if we're running on a platform where we can access HttpOnly cookies, we inject the value in the Authorization header
					token?.let { token ->
						BearerTokens(token, "unused-refresh-token")
					}
				}
			}
		}
	}

	var context = MutableStateFlow(Context(null, User.Role.ANONYMOUS))
		internal set

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

	val templates = Templates(
		this,
		defaultRefCache<Template>()
			.cachedInMemory(context)
			.expireAfter(10.minutes, context),
	)

	val templateVersions = TemplateVersions(
		this,
		defaultRefCache<Template.Version>()
			.cachedInMemory(context)
			.expireAfter(30.minutes, context),
	)

	val forms = Forms(
		this,
		defaultRefCache<Form>()
			.cachedInMemory(context)
			.expireAfter(10.minutes, context)
	)

	val formVersions = FormVersions(
		this,
		defaultRefCache<Form.Version>()
			.cachedInMemory(context)
			.expireAfter(30.minutes, context)
	)

	fun ping(): State<Unit> = state {
		val result = http.request(api2.ping.get, api2.ping.idOf(), Unit, Parameters.Empty, context.value)
		emitAll(result)
	}
}
