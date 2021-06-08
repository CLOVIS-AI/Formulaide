package formulaide.server

import formulaide.db.Database
import formulaide.server.Auth.Companion.Employee
import formulaide.server.routes.userRoutes
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import io.ktor.features.*
import io.ktor.routing.*
import io.ktor.serialization.*

lateinit var database: Database

fun main(args: Array<String>) {
	println("The server is starting…")

	println("Connecting to the database…")
	database = Database("localhost", 27017, "formulaide", "root", "development-password")

	println("Starting Ktor…")
	io.ktor.server.netty.EngineMain.main(args)
}

fun Application.formulaide(@Suppress("UNUSED_PARAMETER") testing: Boolean = false) {

	install(ContentNegotiation) {
		json()
	}

	install(Authentication) {
		jwt(Employee) {
			val auth = Auth(database)
			realm = "formulaide-employee-auth"

			verifier(auth.verifier)
			validate { auth.checkTokenJWT(it.payload) }
		}
	}

	routing {
		userRoutes()
	}
}
