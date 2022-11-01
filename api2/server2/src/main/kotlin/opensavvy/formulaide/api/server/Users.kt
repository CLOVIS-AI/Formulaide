package opensavvy.formulaide.api.server

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.util.date.*
import kotlinx.coroutines.flow.first
import opensavvy.backbone.Ref.Companion.request
import opensavvy.formulaide.api.Context
import opensavvy.formulaide.api.User.TemporaryPassword
import opensavvy.formulaide.api.api2
import opensavvy.formulaide.core.Department
import opensavvy.formulaide.core.User
import opensavvy.formulaide.database.Database
import opensavvy.spine.ktor.server.ContextGenerator
import opensavvy.spine.ktor.server.ResponseStateBuilder
import opensavvy.spine.ktor.server.route
import opensavvy.state.firstValueOrNull
import opensavvy.state.slice.ensureAuthenticated
import opensavvy.state.slice.ensureAuthorized
import java.time.Duration

fun Routing.users(database: Database, contextGenerator: ContextGenerator<Context>, developmentMode: Boolean) {

	route(api2.users.get, contextGenerator) {
		ensureAdministrator { "Seuls les administrateurs peuvent accéder à la liste des utilisateurs" }

		database.users.list(parameters.includeClosed).bind()
			.map { api2.users.id.idOf(it.id) }
	}

	route(api2.users.create, contextGenerator) {
		ensureAdministrator { "Seuls les administrateurs peuvent créer des utilisateurs" }

		val (ref, password) = database.users.create(
			body.email,
			body.name,
			body.departments.mapTo(HashSet()) {
				Department.Ref(
					api2.departments.id.idFrom(it, context).bind(),
					database.departments
				)
			},
			body.administrator,
		).bind()

		api2.users.id.idOf(ref.id) to TemporaryPassword(password)
	}

	route(api2.users.id.get, contextGenerator) {
		val user = ensureEmployee { "Seuls les employés peuvent accéder à leur page" }
		val requestedId = api2.users.id.idFrom(id, context).bind()

		if (user.id != requestedId)
			ensureAdministrator { "Seuls les administrateurs peuvent accéder à la page d'autres utilisateurs qu'eux-même" }

		val result = User.Ref(requestedId, database.users)
			.request().first().bind()

		opensavvy.formulaide.api.User(
			result.email,
			result.name,
			result.open,
			result.departments.mapTo(HashSet()) { dept -> api2.departments.id.idOf(dept.id) },
			result.administrator,
			result.forceResetPassword,
		)
	}

	route(api2.users.id.edit, contextGenerator) {
		ensureAdministrator { "Seuls les administrateurs peuvent modifier les utilisateurs" }

		val requestedId = api2.users.id.idFrom(id, context).bind()
		val ref = User.Ref(requestedId, database.users)

		database.users.edit(
			ref,
			open = body.open,
			administrator = body.administrator,
			departments = body.departments?.mapTo(HashSet()) {
				Department.Ref(
					api2.departments.id.idFrom(it, context).bind(),
					database.departments,
				)
			}
		).bind()
	}

	route(api2.users.id.resetPassword, contextGenerator) {
		ensureAdministrator { "Seuls les administrateurs peuvent réinitialiser le mot de passe d'un utilisateur" }

		val requestedId = api2.users.id.idFrom(id, context).bind()
		val ref = User.Ref(requestedId, database.users)

		val result = database.users.resetPassword(ref).bind()
		TemporaryPassword(result)
	}

	route(api2.users.me.get, contextGenerator) {
		val ref = ensureEmployee { "Seuls les employés peuvent demander leur identifiant" }

		api2.users.id.idOf(ref.id)
	}

	route(api2.users.me.editPassword, contextGenerator) {
		val ref = ensureEmployee { "Seuls les employés peuvent modifier leur mot de passe" }

		database.users.setPassword(ref, body.oldPassword, body.newPassword).bind()
	}

	route(api2.users.me.logIn, contextGenerator) {
		val (ref, token) = database.users.logIn(body.email, body.password).bind()

		call.response.cookies.append(
			name = "session",
			value = "${ref.id}:$token",
			path = "/v2",
			expires = GMTDate() + Duration.ofDays(2).toMillis(),
			httpOnly = true,
			extensions = mapOf(
				"SameSite" to if (developmentMode) "None" else "Strict",
				"Secure" to null,
			)
		)

		val id = api2.users.id.idOf(ref.id)

		val user = ref.request().first().bind()
		id to opensavvy.formulaide.api.User(
			user.email,
			user.name,
			user.open,
			user.departments.mapTo(HashSet()) { dept -> api2.departments.id.idOf(dept.id) },
			user.administrator,
			user.forceResetPassword,
		)
	}

	route(api2.users.me.logOut, contextGenerator) {
		val session = getToken(call)
		ensureAuthenticated(session != null) { "Le cookie de session n'a pas été envoyé" }

		val (id, token) = session.split(':', limit = 2)
		val ref = User.Ref(id, database.users)

		database.users.logOut(ref, token).bind()
	}

}

suspend inline fun ResponseStateBuilder<*, *, Context>.ensureEmployee(lazyMessage: () -> String): User.Ref {
	val ref = context.user
	ensureAuthenticated(ref != null, lazyMessage)
	return ref
}

suspend inline fun ResponseStateBuilder<*, *, Context>.ensureAdministrator(lazyMessage: () -> String): User.Ref {
	val ref = ensureEmployee(lazyMessage)
	ensureAuthorized(context.role >= User.Role.ADMINISTRATOR, lazyMessage)
	return ref
}

private fun getToken(call: ApplicationCall): String? =
	call.request.cookies["session"]
		?: call.request.header("Authorization") // format: 'bearer <session>'
			?.split(' ', limit = 2)
			?.getOrNull(1)

fun context(database: Database) = ContextGenerator { call ->
	val anonymous = Context(user = null, role = User.Role.ANONYMOUS)

	val session = getToken(call)
		?: return@ContextGenerator anonymous
	val (id, token) = session.split(':', limit = 2)

	val ref = User.Ref(id, database.users)

	database.users.verifyToken(ref, token).orNull() ?: return@ContextGenerator anonymous

	val userData = ref.request().firstValueOrNull() ?: return@ContextGenerator anonymous

	Context(
		user = ref,
		role = userData.role,
	)
}
