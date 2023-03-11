package opensavvy.formulaide.backend

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.hsts.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.*
import kotlinx.datetime.Clock
import opensavvy.formulaide.core.Auth.Companion.currentRole
import opensavvy.formulaide.core.Auth.Companion.currentUser
import opensavvy.formulaide.fake.FakeFiles
import opensavvy.formulaide.fake.FakeForms
import opensavvy.formulaide.fake.FakeRecords
import opensavvy.formulaide.fake.FakeTemplates
import opensavvy.formulaide.mongo.Database
import opensavvy.formulaide.mongo.DepartmentDb
import opensavvy.formulaide.mongo.UserDb
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

	val departments = DepartmentDb(database, cacheScope.coroutineContext)
	val users = UserDb(database, cacheScope.coroutineContext, departments)
	val templates = FakeTemplates(clock)
	val forms = FakeForms(clock)
	val files = FakeFiles(clock)
	val records = FakeRecords(clock, files)

	configureServer()

	install(CallLogging) {
		level = Level.INFO
	}

	install(TokenAuthentication) {
		this.users = users
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
