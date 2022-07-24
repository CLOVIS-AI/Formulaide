package formulaide.client

import formulaide.api.users.User
import formulaide.client.Client.Anonymous
import formulaide.client.Client.Authenticated
import formulaide.client.bones.Departments
import formulaide.client.bones.Users
import formulaide.client.files.MultipartUpload
import formulaide.client.routes.getMe
import formulaide.core.Department
import formulaide.core.Ref
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import opensavvy.backbone.Cache
import opensavvy.backbone.cache.MemoryCache.Companion.cachedInMemory

/**
 * Common behavior between [Anonymous] and [Authenticated].
 */
sealed class Client(
	val hostUrl: String,
) {

	//region Backbones

	@Suppress("LeakingThis")
	val departments = Departments(
		this,
		Cache.Default<Department>()
			.cachedInMemory()
	)

	@Suppress("LeakingThis")
	val users = Users(
		this,
		Cache.Default<formulaide.core.User>()
			.cachedInMemory()
	)

	//endregion
	//region Methods

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

	internal suspend inline fun <reified Out> patch(
		url: String,
		body: Any? = null,
		block: HttpRequestBuilder.() -> Unit = {},
	) = request<Out>(HttpMethod.Patch, url, body, block)

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

	//endregion
	//region HTTP client

	protected open fun configure(client: HttpClientConfig<*>) = with(client) {
		install(ContentNegotiation) {
			json(Json(jsonSerializer) {
				serializersModule = SerializersModule {
					contextual(Ref.Serializer(departments))
				}
			})
		}

		expectSuccess = true
	}

	internal val client = HttpClient {
		configure(this)
	}

	//endregion

	companion object {

		internal val jsonSerializer = DefaultJson

	}

	class Anonymous private constructor(hostUrl: String) : Client(hostUrl) {
		companion object {
			fun connect(hostUrl: String) = Anonymous(hostUrl)
		}

		suspend fun authenticate(token: String) = Authenticated.connect(hostUrl, token)
	}

	class Authenticated private constructor(private val token: String, hostUrl: String) : Client(hostUrl) {

		/**
		 * The profile of the user we are connected as.
		 */
		lateinit var me: User
			private set

		override fun configure(client: HttpClientConfig<*>) = with(client) {
			super.configure(client)

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

		/**
		 * Disconnects this client.
		 *
		 * Calling this method is important because it will clear the cookies set by the login.
		 */
		suspend fun logout() = post<String>("/api/auth/logout")

		companion object {
			suspend fun connect(hostUrl: String, token: String) =
				Authenticated(token, hostUrl).apply {
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
