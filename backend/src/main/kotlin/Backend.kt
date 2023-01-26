package opensavvy.formulaide.backend

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.hsts.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import opensavvy.formulaide.core.Auth.Companion.currentRole
import opensavvy.formulaide.core.Auth.Companion.currentUser
import opensavvy.formulaide.fake.FakeDepartments
import opensavvy.formulaide.fake.FakeForms
import opensavvy.formulaide.fake.FakeTemplates
import opensavvy.formulaide.fake.FakeUsers
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

	val departments = FakeDepartments()
	val users = FakeUsers()
	val templates = FakeTemplates(clock)
	val forms = FakeForms(clock)

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
	}
}
