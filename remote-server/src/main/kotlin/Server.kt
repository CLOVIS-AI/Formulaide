package opensavvy.formulaide.server

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import opensavvy.formulaide.core.Auth
import opensavvy.formulaide.remote.ApiJson
import opensavvy.spine.ktor.server.ContextGenerator

fun Application.configureServer() {
	install(Routing)

	install(ContentNegotiation) {
		json(ApiJson)
	}
}

data class AuthPrincipal(val auth: Auth) : Principal

internal val contextGenerator = ContextGenerator {}
