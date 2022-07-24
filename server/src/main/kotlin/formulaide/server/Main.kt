package formulaide.server

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import formulaide.api.bones.ApiNewUser
import formulaide.api.data.Config
import formulaide.api.types.Email
import formulaide.core.Ref
import formulaide.db.Database
import formulaide.db.document.findUser
import formulaide.server.Auth.Companion.Employee
import formulaide.server.routes.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.conditionalheaders.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import opensavvy.backbone.Ref.Companion.requestValue
import org.slf4j.LoggerFactory

// New job: the server never dies cleanly, it can only be killed. No need for structure concurrency.
val database = Database("localhost", 27017, "formulaide", "root", "development-password", Job())
val allowUnsafeCookie = System.getenv("formulaide_allow_unsafe_cookie").toBoolean()

const val rootServiceName = "Service informatique"
const val rootUser = "admin@formulaide"
const val rootPassword = "admin-development-password"

fun main(args: Array<String>) {
	println("Starting up; CLI arguments: ${args.contentDeepToString()}")

	runBlocking {
		println("Checking that the admin user exists…")
		val department = database.departments.all()
			.map { it.requestValue() }
			.firstOrNull { it.name == rootServiceName }
			?: database.departments.create(rootServiceName).requestValue()

		val auth = Auth(database)

		if (database.findUser(rootUser) == null) {
			println("Creating the administrator account $rootUser…")
			auth.newAccount(
				ApiNewUser(
					rootUser,
					"Administrateur",
					setOf(department.id.toInt()),
					true,
					rootPassword,
				)
			)
		} else {
			println("The administrator account ($rootUser) already exists.")
		}
	}

	println("Disable MongoDB request logging…")
	val loggerContext = LoggerFactory.getILoggerFactory() as LoggerContext
	val rootLogger = loggerContext.getLogger("org.mongodb.driver")
	rootLogger.level = Level.INFO

	println("Starting Ktor…")
	io.ktor.server.netty.EngineMain.main(args)
}

val serializer = Json(DefaultJson) {
	useArrayPolymorphism = false
}

@Suppress("unused") // see application.conf
fun Application.formulaide(@Suppress("UNUSED_PARAMETER") testing: Boolean = false) {
	val auth = Auth(database)

	if (developmentMode)
		System.err.println("WARNING. The server is running in development mode. This is NOT safe for production. See https://ktor.io/docs/development-mode.html")
	if (allowUnsafeCookie)
		System.err.println("WARNING. The server has been allowed to create non-safe HTTP cookies. Remove the environment variable 'formulaide_allow_unsafe_cookie' for production use.")

	install(ContentNegotiation) {
		json(Json(serializer) {
			serializersModule = SerializersModule {
				contextual(Ref.Serializer(database.departments))
			}
		})
	}

	install(CallLogging) {
		level = org.slf4j.event.Level.DEBUG
	}

	install(CORS) { //TODO: audit
		anyHost()
		allowCredentials = true
		allowSameOrigin = true
		allowHeader("Accept")
		allowHeader(HttpHeaders.ContentType)
		allowHeader(HttpHeaders.Authorization)
		allowMethod(HttpMethod.Options)
		allowMethod(HttpMethod.Put)
		allowMethod(HttpMethod.Patch)
		allowMethod(HttpMethod.Delete)
	}

	install(Authentication) {
		jwt(Employee) {
			realm = "formulaide-employee-auth"

			verifier(auth.verifier)
			validate { auth.checkTokenJWT(it.payload) }
		}
	}

	install(ConditionalHeaders)

	install(StatusPages) {
		exception<Throwable> { call, error ->
			call.respondText(error.message ?: error.toString(), status = HttpStatusCode.BadRequest)
			error.printStackTrace()
		}
	}

	routing {
		staticFrontendRoutes()

		with(AuthRouting) { enable(auth) }
		with(UserRouting) { enable(auth) }
		with(DepartmentRouting) { enable() }
		dataRoutes()
		formRoutes()
		submissionRoutes()
		fileRoutes()
		alertRoutes()

		get("/config") {
			call.respond(
				Config(
					reportEmail = System.getenv("formulaide_support_email")?.let { Email(it) },
					helpURL = System.getenv("formulaide_help_url"),
					pdfLeftImageURL = System.getenv("formulaide_pdf_image_left_url")?.takeIf { it.isNotBlank() },
					pdfRightImageURL = System.getenv("formulaide_pdf_image_right_url")?.takeIf { it.isNotBlank() },
				)
			)
		}
	}
}
