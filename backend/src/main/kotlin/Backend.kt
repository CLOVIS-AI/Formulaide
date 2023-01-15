package opensavvy.formulaide.backend

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.hsts.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.Clock
import opensavvy.backbone.Ref.Companion.now
import opensavvy.formulaide.core.Auth
import opensavvy.formulaide.core.User
import opensavvy.formulaide.core.User.Role.Companion.role
import opensavvy.formulaide.fake.FakeDepartments
import opensavvy.formulaide.fake.FakeForms
import opensavvy.formulaide.fake.FakeTemplates
import opensavvy.formulaide.fake.FakeUsers
import opensavvy.formulaide.server.*
import opensavvy.state.outcome.out
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

	install(Authentication) {
		bearer {
			authenticate { bearer ->
				out {
					val user = User.Ref(bearer.token, users)
					val role = user.now().bind().role
					AuthPrincipal(Auth(role, user))
				}.tapLeft { application.log.warn("Could not authenticate user '$bearer': $it") }
					.orNull()
			}
		}
	}

	if (!developmentMode) {
		install(HSTS) {
			includeSubDomains = true
		}
	} else {
		log.error("DEVELOPMENT MODE IS ENABLED. THIS IS NOT SAFE FOR PRODUCTION.")
	}

	routing {
		get("ping") {
			call.respondText("Pong")
		}

		departments(departments)
		users(users, departments)
		templates(templates)
		forms(departments, templates, forms)
	}
}
