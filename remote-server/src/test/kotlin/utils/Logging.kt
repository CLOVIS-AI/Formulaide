package opensavvy.formulaide.remote.server.utils

import io.ktor.client.*
import io.ktor.client.plugins.logging.*
import io.ktor.server.application.*
import io.ktor.server.plugins.callloging.*
import org.slf4j.event.Level

fun Application.configureTestLogging() {
	install(CallLogging) {
		level = Level.INFO
	}
}

fun HttpClientConfig<*>.configureTestLogging() {
	install(Logging) {
		logger = Logger.DEFAULT
		level = LogLevel.ALL
	}
}
