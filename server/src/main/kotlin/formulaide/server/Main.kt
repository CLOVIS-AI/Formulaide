package formulaide.server

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*

val message get() = "Hello World!"

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

fun Application.formulaide(testing: Boolean = false) {
	routing {
		get("/") {
			call.respondText("Hello, world!")
		}
	}
}
