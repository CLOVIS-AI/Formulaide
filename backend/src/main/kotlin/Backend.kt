package opensavvy.formulaide.backend

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import opensavvy.formulaide.server.configureServer
import org.slf4j.event.Level

fun main() {
	embeddedServer(
		Netty,
		port = 9000,
		module = Application::formulaide,
	).start(wait = true)
}

fun Application.formulaide() {
	configureServer()

	install(CallLogging) {
		level = Level.INFO
	}

	if (developmentMode) {
		log.error("DEVELOPMENT MODE IS ENABLED. THIS IS NOT SAFE FOR PRODUCTION.")
	}

	routing {
		get("ping") {
			call.respondText("Pong")
		}
	}
}
