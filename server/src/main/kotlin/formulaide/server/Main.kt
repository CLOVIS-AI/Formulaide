package formulaide.server

import formulaide.api.types.Email
import formulaide.api.types.Ref
import formulaide.api.users.NewUser
import formulaide.api.users.User
import formulaide.db.Database
import formulaide.db.document.createService
import formulaide.server.Auth.Companion.Employee
import formulaide.server.routes.*
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.serialization.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

val database = Database("localhost", 27017, "formulaide", "root", "development-password")

fun main(args: Array<String>) {
	if (args.isNotEmpty() && args[0] == "--init") {
		require(System.getenv("formulaide_allow_init") == "true") { "For security reasons, you need to set the 'formulaide_allow_init' environment variable to 'true' before you initialize the database." }

		runBlocking {
			val service = database.createService("Service informatique")
			val auth = Auth(database)
			auth.newAccount(NewUser("admin-development-password",
			                        User(Email("admin@formulaide"),
			                             "Administrateur",
			                             Ref(service.id.toString()),
			                             true)))
			auth.newAccount(NewUser("employee-development-password",
			                        User(Email("employee@formulaide"),
			                             "Employé",
			                             Ref(service.id.toString()),
			                             false)))
		}
	}

	println("The server is starting…")

	println("Starting Ktor…")
	io.ktor.server.netty.EngineMain.main(args)
}

@Suppress("unused") // see application.conf
fun Application.formulaide(@Suppress("UNUSED_PARAMETER") testing: Boolean = false) {
	val auth = Auth(database)

	install(ContentNegotiation) {
		json(Json(DefaultJson) {
			useArrayPolymorphism = false
		})
	}

	install(CORS) { //TODO: audit
		anyHost()
		allowCredentials = true
		header("Accept")
		header("Content-Type")
		header("Authorization")
	}

	install(Authentication) {
		jwt(Employee) {
			realm = "formulaide-employee-auth"

			verifier(auth.verifier)
			validate { auth.checkTokenJWT(it.payload) }
		}
	}

	install(StatusPages) {
		exception<Throwable> { error ->
			call.respondText(error.message ?: error.toString(), status = HttpStatusCode.BadRequest)
		}
	}

	routing {
		staticFrontendRoutes()

		userRoutes(auth)
		serviceRoutes()
		dataRoutes()
		formRoutes()
		submissionRoutes()
	}
}
