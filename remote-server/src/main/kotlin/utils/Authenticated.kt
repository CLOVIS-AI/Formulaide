package opensavvy.formulaide.server.utils

import io.ktor.server.auth.*
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.withContext
import opensavvy.formulaide.core.Auth
import opensavvy.formulaide.server.AuthPrincipal
import opensavvy.spine.ktor.server.ResponseStateBuilder

suspend fun <R> ResponseStateBuilder<*, *, *>.authenticated(block: suspend () -> R): R {
	val auth = call.principal<AuthPrincipal>()?.auth ?: Auth.Guest

	return withContext(
		auth + CoroutineName("${auth.user?.id}(${auth.role})")
	) { block() }
}
