package formulaide.server.routes

import io.ktor.application.*
import io.ktor.http.content.*
import io.ktor.response.*
import io.ktor.routing.*

fun Routing.staticFrontendRoutes() {
	get("/") {
		call.respondRedirect("/front/index.html", permanent = true)
	}

	static("/front") {
		resources("front")
	}
}
