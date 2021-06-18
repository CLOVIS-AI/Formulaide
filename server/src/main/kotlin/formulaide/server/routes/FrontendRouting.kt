package formulaide.server.routes

import io.ktor.http.content.*
import io.ktor.routing.*

fun Routing.staticFrontendRoutes() {
	static("/front") {
		resources("front")
	}
}
