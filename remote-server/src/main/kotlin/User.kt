package opensavvy.formulaide.remote.server

import io.ktor.server.routing.*
import opensavvy.backbone.Ref.Companion.now
import opensavvy.formulaide.core.Department
import opensavvy.formulaide.core.User
import opensavvy.formulaide.core.data.Email
import opensavvy.formulaide.core.data.Password
import opensavvy.formulaide.core.data.Token
import opensavvy.formulaide.remote.api
import opensavvy.formulaide.remote.dto.UserDto
import opensavvy.spine.Identified
import opensavvy.spine.ktor.server.route

fun Routing.users(users: User.Service, departments: Department.Service) {

	route(api.users.get, contextGenerator) {
		users.list(includeClosed = parameters.includeClosed)
			.bind()
			.map { api.users.id.idOf(it.id) }
	}

	route(api.users.id.get, contextGenerator) {
		val user = api.users.id.refOf(id, users).bind()
			.now().bind()

		UserDto(
			email = user.email.value,
			name = user.name,
			active = user.active,
			administrator = user.administrator,
			departments = user.departments.mapTo(HashSet()) {
				api.departments.id.idOf(it.id)
			},
			singleUsePassword = user.singleUsePassword,
		)
	}

	route(api.users.create, contextGenerator) {
		users.create(
			email = Email(body.email),
			fullName = body.name,
			administrator = body.administrator,
		).bind()
			.let { (ref, password) -> Identified(api.users.id.idOf(ref.id), password.value) }
	}

	route(api.users.logIn, contextGenerator) {
		users.logIn(
			email = Email(body.email),
			password = Password(body.password),
		).bind()
			.let { (ref, token) -> Identified(api.users.id.idOf(ref.id), token.value) }
	}

	route(api.users.id.token.verify, contextGenerator) {
		users.verifyToken(
			user = api.users.id.refOf(id, users).bind(),
			token = Token(body),
		).bind()
	}

	route(api.users.id.token.logOut, contextGenerator) {
		users.logOut(
			user = api.users.id.refOf(id, users).bind(),
			token = Token(body),
		).bind()
	}

	route(api.users.id.edit, contextGenerator) {
		users.edit(
			user = api.users.id.refOf(id, users).bind(),
			active = body.active,
			administrator = body.administrator,
		).bind()
	}

	route(api.users.id.departments.get, contextGenerator) {
		api.users.id.refOf(id, users).bind()
			.now().bind()
			.departments.mapTo(HashSet()) {
				api.departments.id.idOf(it.id)
			}
	}

	route(api.users.id.departments.add, contextGenerator) {
		users.join(
			user = api.users.id.refOf(id, users).bind(),
			department = api.departments.id.refOf(body, departments).bind(),
		).bind()
	}

	route(api.users.id.departments.remove, contextGenerator) {
		users.leave(
			user = api.users.id.refOf(id, users).bind(),
			department = api.departments.id.refOf(body, departments).bind(),
		).bind()
	}

	route(api.users.id.password.reset, contextGenerator) {
		users.resetPassword(
			user = api.users.id.refOf(id, users).bind(),
		).bind().value
	}

	route(api.users.id.password.set, contextGenerator) {
		users.setPassword(
			user = api.users.id.refOf(id, users).bind(),
			oldPassword = body.oldPassword,
			newPassword = Password(body.newPassword),
		).bind()
			.let { Identified(id, Unit) }
	}

	route(api.users.id.token.verify, contextGenerator) {
		users.verifyToken(
			user = api.users.id.refOf(id, users).bind(),
			token = Token(body),
		).bind()
	}

	route(api.users.id.token.logOut, contextGenerator) {
		users.logOut(
			user = api.users.id.refOf(id, users).bind(),
			token = Token(body),
		).bind()
	}

}
