package formulaide.server

import formulaide.db.Database
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*

lateinit var database: Database

fun main(args: Array<String>) {
	println("The server is starting…")

	println("Connecting to the database…")
	database = Database("localhost", 27017, "formulaide", "root", "development-password")

	println("Starting Ktor…")
	io.ktor.server.netty.EngineMain.main(args)
}

fun Application.formulaide(testing: Boolean = false) {
	routing {
		get("/") {
			call.respondText("Hello, world!")
		}
	}
}
