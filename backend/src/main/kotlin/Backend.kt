package opensavvy.formulaide.backend

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.hsts.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.*
import kotlinx.datetime.Clock
import opensavvy.formulaide.core.currentRole
import opensavvy.formulaide.core.currentUser
import opensavvy.formulaide.fake.FakeFiles
import opensavvy.formulaide.fake.FakeRecords
import opensavvy.formulaide.mongo.*
import opensavvy.formulaide.remote.server.*
import org.slf4j.event.Level

fun main() {
	embeddedServer(
		Netty,
		port = 9000,
		module = Application::formulaide,
	).start(wait = true)
}

fun Application.formulaide() {
	val clock = Clock.System

	val database = Database(
		hostname = System.getenv("formulaide_host"),
		port = System.getenv("formulaide_port"),
		username = System.getenv("formulaide_username"),
		password = System.getenv("formulaide_password"),
		database = System.getenv("formulaide_database"),
	)

	val cacheScope = CoroutineScope(Job())

	val departments = MongoDepartments(database, cacheScope)
	val users = MongoUsers(database, cacheScope, departments)
	val templates = MongoTemplate(database, cacheScope, clock)
	val forms = MongoForms(database, cacheScope, departments, templates, clock)
	val files = FakeFiles(clock)
	val records = FakeRecords(clock, files)

	configureServer()

	install(CallLogging) {
		level = Level.INFO
	}

	install(TokenAuthentication) {
		this.users = users
	}

	install(StatusPages) {
		exception<BadRequestException> { call: ApplicationCall, cause: BadRequestException ->
			if (cause.cause?.cause is IllegalArgumentException) {
				call.respond(HttpStatusCode.UnprocessableEntity, cause.cause?.cause?.message ?: "Pas de message")
			} else if (cause.cause is IllegalArgumentException) {
				call.respond(HttpStatusCode.BadRequest, cause.cause?.message ?: "Pas de message")
			} else {
				call.respond(HttpStatusCode.BadRequest, cause.message ?: "Pas de message")
			}

			logError(call, cause)
		}
	}

	if (!developmentMode) {
		install(HSTS) {
			includeSubDomains = true
		}
	} else {
		log.error("DEVELOPMENT MODE IS ENABLED. THIS IS NOT SAFE FOR PRODUCTION.")
	}

	@OptIn(DelicateCoroutinesApi::class) // These requests are fire-and-forget and will not last long
	GlobalScope.launch { createDefaultUsers(users) }

	routing {
		get("ping") {
			call.respondText("Hi! You are ${currentUser()} (role: ${currentRole()})")
		}

		departments(departments)
		users(users, departments)
		templates(templates)
		forms(departments, templates, forms)
		records(users, forms, records.submissions, records)
	}
}
