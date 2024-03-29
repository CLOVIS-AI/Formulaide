package opensavvy.formulaide.remote.server

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import opensavvy.formulaide.remote.ApiJson
import opensavvy.spine.ktor.server.ContextGenerator

fun Application.configureServer() {
	install(Routing)

	install(ContentNegotiation) {
		json(ApiJson)
	}
}

internal val contextGenerator = ContextGenerator {}
