package opensavvy.formulaide.remote.server

import io.ktor.server.routing.*
import opensavvy.backbone.now
import opensavvy.formulaide.core.Department
import opensavvy.formulaide.core.User
import opensavvy.formulaide.core.data.Email
import opensavvy.formulaide.core.data.Password
import opensavvy.formulaide.core.data.Token
import opensavvy.formulaide.remote.api
import opensavvy.formulaide.remote.dto.UserDto
import opensavvy.formulaide.remote.server.utils.invalidRequest
import opensavvy.formulaide.remote.server.utils.notFound
import opensavvy.formulaide.remote.server.utils.unauthenticated
import opensavvy.formulaide.remote.server.utils.unauthorized
import opensavvy.spine.Identified
import opensavvy.spine.ktor.server.route
import opensavvy.state.arrow.toEither
import opensavvy.state.outcome.mapFailure

fun Routing.users(users: User.Service<*>, departments: Department.Service<*>) {

	route(api.users.get, contextGenerator) {
		users.list(includeClosed = parameters.includeClosed)
			.mapFailure {
				when (it) {
					User.Failures.Unauthenticated -> unauthenticated()
					User.Failures.Unauthorized -> unauthorized()
				}
			}
			.toEither()
			.bind()
			.map { api.users.id.idOf(it.toIdentifier().text) }
	}

	route(api.users.id.get, contextGenerator) {
		val ref = api.users.id.identifierOf(id)
			.let(users::fromIdentifier)

		val user = ref.now()
			.mapFailure {
				when (it) {
					is User.Failures.NotFound -> notFound(ref)
					User.Failures.Unauthenticated -> unauthenticated()
					User.Failures.Unauthorized -> unauthorized()
				}
			}
			.toEither()
			.bind()

		UserDto(
			email = user.email.value,
			name = user.name,
			active = user.active,
			administrator = user.administrator,
			departments = user.departments.mapTo(HashSet()) {
				api.departments.id.idOf(it.toIdentifier().text)
			},
			singleUsePassword = user.singleUsePassword,
		)
	}

	route(api.users.create, contextGenerator) {
		users.create(
			email = Email(body.email),
			fullName = body.name,
			administrator = body.administrator,
		).mapFailure {
			when (it) {
				User.Failures.Unauthenticated -> unauthenticated()
				User.Failures.Unauthorized -> unauthorized()
				is User.Failures.UserAlreadyExists -> invalidRequest(UserDto.NewFailures.UserAlreadyExists(it.email.value))
			}
		}.toEither()
			.bind()
			.let { (ref, password) -> Identified(api.users.id.idOf(ref.toIdentifier().text), password.value) }
	}

	route(api.users.logIn, contextGenerator) {
		users.logIn(
			email = Email(body.email),
			password = Password(body.password),
		).mapFailure {
			when (it) {
				User.Failures.IncorrectCredentials -> invalidRequest(UserDto.LogInFailures)
			}
		}.toEither()
			.bind()
			.let { (ref, token) -> Identified(api.users.id.idOf(ref.toIdentifier().text), token.value) }
	}

	route(api.users.id.token.verify, contextGenerator) {
		val user = api.users.id.identifierOf(id)
			.let(users::fromIdentifier)

		user.verifyToken(Token(body))
			.mapFailure {
				when (it) {
					User.Failures.IncorrectCredentials -> invalidRequest(UserDto.TokenVerifyFailures)
				}
			}
			.toEither()
			.bind()
	}

	route(api.users.id.token.logOut, contextGenerator) {
		val user = api.users.id.identifierOf(id)
			.let(users::fromIdentifier)

		user.logOut(Token(body))
			.mapFailure {
				when (it) {
					is User.Failures.NotFound -> notFound(user)
					User.Failures.Unauthenticated -> unauthenticated()
					User.Failures.Unauthorized -> unauthorized()
				}
			}
			.toEither()
			.bind()
	}

	route(api.users.id.edit, contextGenerator) {
		val user = api.users.id.identifierOf(id)
			.let(users::fromIdentifier)

		user.edit(
			active = body.active,
			administrator = body.administrator,
		).mapFailure {
			when (it) {
				User.Failures.CannotEditYourself -> invalidRequest(UserDto.EditFailures.CannotEditYourself)
				is User.Failures.NotFound -> notFound(user)
				User.Failures.Unauthenticated -> unauthenticated()
				User.Failures.Unauthorized -> unauthorized()
			}
		}.toEither()
			.bind()
	}

	route(api.users.id.departments.get, contextGenerator) {
		val user = api.users.id.identifierOf(id)
			.let(users::fromIdentifier)

		user.now()
			.mapFailure {
				when (it) {
					is User.Failures.NotFound -> notFound(user)
					User.Failures.Unauthenticated -> unauthenticated()
					User.Failures.Unauthorized -> unauthorized()
				}
			}
			.toEither()
			.bind()
			.departments
			.mapTo(HashSet()) { api.departments.id.idOf(it.toIdentifier().text) }
	}

	route(api.users.id.departments.add, contextGenerator) {
		val user = api.users.id.identifierOf(id)
			.let(users::fromIdentifier)

		user.join(
			departments.fromIdentifier(api.departments.id.identifierOf(body)),
		).mapFailure {
			when (it) {
				is User.Failures.NotFound -> notFound(user)
				User.Failures.Unauthenticated -> unauthenticated()
				User.Failures.Unauthorized -> unauthorized()
			}
		}.toEither()
			.bind()
	}

	route(api.users.id.departments.remove, contextGenerator) {
		val user = api.users.id.identifierOf(id)
			.let(users::fromIdentifier)

		user.leave(
			departments.fromIdentifier(api.departments.id.identifierOf(body)),
		).mapFailure {
			when (it) {
				is User.Failures.NotFound -> notFound(user)
				User.Failures.Unauthenticated -> unauthenticated()
				User.Failures.Unauthorized -> unauthorized()
			}
		}.toEither()
			.bind()
	}

	route(api.users.id.password.reset, contextGenerator) {
		val user = api.users.id.identifierOf(id)
			.let(users::fromIdentifier)

		user.resetPassword()
			.mapFailure {
				when (it) {
					is User.Failures.NotFound -> notFound(user)
					User.Failures.Unauthenticated -> unauthenticated()
					User.Failures.Unauthorized -> unauthorized()
				}
			}
			.toEither()
			.bind()
			.value
	}

	route(api.users.id.password.set, contextGenerator) {
		val user = api.users.id.identifierOf(id)
			.let(users::fromIdentifier)

		user.setPassword(
			oldPassword = body.oldPassword,
			newPassword = Password(body.newPassword),
		).mapFailure {
			when (it) {
				User.Failures.CanOnlySetYourOwnPassword -> invalidRequest(UserDto.PasswordSetFailures.CanOnlySetYourOwnPassword)
				User.Failures.IncorrectPassword -> invalidRequest(UserDto.PasswordSetFailures.IncorrectOldPassword)
				is User.Failures.NotFound -> notFound(user)
				User.Failures.Unauthenticated -> unauthenticated()
			}
		}.toEither()
			.bind()
			.let { Identified(id, Unit) }
	}

}
