package opensavvy.formulaide.api.server

import io.ktor.server.routing.*
import io.ktor.util.date.*
import kotlinx.coroutines.flow.emitAll
import opensavvy.backbone.Ref.Companion.request
import opensavvy.formulaide.api.Context
import opensavvy.formulaide.api.User.TemporaryPassword
import opensavvy.formulaide.api.api2
import opensavvy.formulaide.core.Department
import opensavvy.formulaide.core.User
import opensavvy.formulaide.database.Database
import opensavvy.formulaide.state.bind
import opensavvy.formulaide.state.mapSuccess
import opensavvy.formulaide.state.onEachNotSuccess
import opensavvy.spine.ktor.server.ContextGenerator
import opensavvy.spine.ktor.server.ResponseStateBuilder
import opensavvy.spine.ktor.server.route
import opensavvy.state.Slice.Companion.successful
import opensavvy.state.ensureAuthenticated
import opensavvy.state.ensureAuthorized
import opensavvy.state.firstResultOrNull
import java.time.Duration

fun Routing.users(database: Database, contextGenerator: ContextGenerator<Context>, developmentMode: Boolean) {

	route(api2.users.get, contextGenerator) {
		ensureAdministrator { "Seuls les administrateurs peuvent accéder à la liste des utilisateurs" }

		val result = database.users.list(parameters.includeClosed)
			.mapSuccess { list -> list.map { api2.users.id.idOf(it.id) } }

		emitAll(result)
	}

	route(api2.users.create, contextGenerator) {
		ensureAdministrator { "Seuls les administrateurs peuvent créer des utilisateurs" }

		val result = database.users.create(
			body.email,
			body.name,
			body.departments.mapTo(HashSet()) {
				Department.Ref(
					bind(api2.departments.id.idFrom(it, context)),
					database.departments
				)
			},
			body.administrator,
		)
			.mapSuccess { (ref, password) -> api2.users.id.idOf(ref.id) to TemporaryPassword(password) }

		emitAll(result)
	}

	route(api2.users.id.get, contextGenerator) {
		val user = ensureEmployee { "Seuls les employés peuvent accéder à leur page" }
		val requestedId = bind(api2.users.id.idFrom(id, context))

		if (user.id != requestedId)
			ensureAdministrator { "Seuls les administrateurs peuvent accéder à la page d'autres utilisateurs qu'eux-même" }

		val result = User.Ref(requestedId, database.users)
			.request()
			.mapSuccess {
				opensavvy.formulaide.api.User(
					it.email,
					it.name,
					it.open,
					it.departments.mapTo(HashSet()) { dept -> api2.departments.id.idOf(dept.id) },
					it.administrator,
					it.forceResetPassword,
				)
			}

		emitAll(result)
	}

	route(api2.users.id.edit, contextGenerator) {
		ensureAdministrator { "Seuls les administrateurs peuvent modifier les utilisateurs" }

		val requestedId = bind(api2.users.id.idFrom(id, context))
		val ref = User.Ref(requestedId, database.users)

		val result = database.users.edit(
			ref,
			open = body.open,
			administrator = body.administrator,
			departments = body.departments?.mapTo(HashSet()) {
				Department.Ref(
					bind(api2.departments.id.idFrom(it, context)),
					database.departments,
				)
			}
		)

		emitAll(result)
	}

	route(api2.users.id.resetPassword, contextGenerator) {
		ensureAdministrator { "Seuls les administrateurs peuvent réinitialiser le mot de passe d'un utilisateur" }

		val requestedId = bind(api2.users.id.idFrom(id, context))
		val ref = User.Ref(requestedId, database.users)

		val result = database.users.resetPassword(ref)
			.mapSuccess { TemporaryPassword(it) }

		emitAll(result)
	}

	route(api2.users.me.get, contextGenerator) {
		val ref = ensureEmployee { "Seuls les employés peuvent demander leur identifiant" }

		emit(successful(api2.users.id.idOf(ref.id)))
	}

	route(api2.users.me.editPassword, contextGenerator) {
		val ref = ensureEmployee { "Seuls les employés peuvent modifier leur mot de passe" }

		val result = database.users.setPassword(ref, body.oldPassword, body.newPassword)

		emitAll(result)
	}

	route(api2.users.me.logIn, contextGenerator) {
		val (ref, token) = database.users.logIn(body.email, body.password)
			.onEachNotSuccess { emit(it) }
			.firstResultOrNull()
			?: return@route

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

		emit(successful(api2.users.id.idOf(ref.id) to Unit))
	}

	route(api2.users.me.logOut, contextGenerator) {
		val session = call.request.cookies["session"]
		ensureAuthenticated(session != null) { "Le cookie de session n'a pas été envoyé" }

		val (id, token) = session.split(':', limit = 2)
		val ref = User.Ref(id, database.users)

		emitAll(database.users.logOut(ref, token))
	}

}

suspend inline fun ResponseStateBuilder<*, *, *, Context>.ensureEmployee(lazyMessage: () -> String): User.Ref {
	val ref = context.user
	ensureAuthenticated(ref != null, lazyMessage)
	return ref
}

suspend inline fun ResponseStateBuilder<*, *, *, Context>.ensureAdministrator(lazyMessage: () -> String): User.Ref {
	val ref = ensureEmployee(lazyMessage)
	ensureAuthorized(context.role >= User.Role.ADMINISTRATOR, lazyMessage)
	return ref
}

fun context(database: Database) = ContextGenerator { call ->
	val anonymous = Context(user = null, role = User.Role.ANONYMOUS)

	val session = call.request.cookies["session"] ?: return@ContextGenerator anonymous
	val (id, token) = session.split(':', limit = 2)

	val ref = User.Ref(id, database.users)

	database.users.verifyToken(ref, token)
		.firstResultOrNull()
		?: return@ContextGenerator anonymous

	val userData = ref.request()
		.firstResultOrNull()
		?: return@ContextGenerator anonymous

	Context(
		user = ref,
		role = userData.role,
	)
}
