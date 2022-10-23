package opensavvy.formulaide.api.server

import io.ktor.server.routing.*
import opensavvy.formulaide.api.Context
import opensavvy.formulaide.api.api2
import opensavvy.spine.ktor.server.ContextGenerator
import opensavvy.spine.ktor.server.route
import opensavvy.state.Slice.Companion.successful

fun Routing.ping(context: ContextGenerator<Context>) {

	route(api2.ping.get, context) {
		emit(successful(Unit))
	}

}
