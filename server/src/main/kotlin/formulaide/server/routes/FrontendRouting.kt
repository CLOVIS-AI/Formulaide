package formulaide.server.routes

import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Routing.staticFrontendRoutes() {
	get("/") {
		call.respondRedirect("/front/index.html", permanent = true)
	}

	static("/front") {
		resources("front")
	}
}
