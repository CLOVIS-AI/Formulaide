package opensavvy.formulaide.remote.client

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.job
import opensavvy.cache.contextual.cache
import opensavvy.cache.contextual.cachedInMemory
import opensavvy.cache.contextual.expireAfter
import opensavvy.formulaide.core.Department
import opensavvy.formulaide.core.User
import opensavvy.formulaide.core.currentRole
import opensavvy.formulaide.core.data.Email
import opensavvy.formulaide.core.data.Password
import opensavvy.formulaide.core.data.Token
import opensavvy.formulaide.core.utils.Identifier
import opensavvy.formulaide.remote.api
import opensavvy.formulaide.remote.client.RemoteUsers.Ref
import opensavvy.formulaide.remote.dto.UserDto
import opensavvy.spine.Parameters
import opensavvy.spine.SpineFailure
import opensavvy.spine.ktor.client.request
import opensavvy.state.arrow.out
import opensavvy.state.coroutines.ProgressiveFlow
import opensavvy.state.outcome.Outcome
import opensavvy.state.outcome.mapFailure
import kotlin.time.Duration.Companion.minutes

class RemoteUsers(
	private val client: Client,
	scope: CoroutineScope,
	private val departments: Department.Service<*>,
) : User.Service<Ref> {

	private val cache = cache<Ref, User.Role, User.Failures.Get, User> { ref, role ->
		out {
			val result = client.http.request(
				api.users.id.get,
				api.users.id.idOf(ref.id),
				Unit,
				Parameters.Empty,
				Unit,
			).mapFailure {
				when (it.type) {
					SpineFailure.Type.Unauthenticated -> User.Failures.Unauthenticated
					SpineFailure.Type.Unauthorized -> User.Failures.Unauthorized
					SpineFailure.Type.NotFound -> User.Failures.NotFound(ref)
					else -> error("Received an unexpected status: $it")
				}
			}.bind()

			User(
				email = Email(result.email),
				name = result.name,
				active = result.active,
				administrator = result.administrator,
				departments = result.departments.mapTo(HashSet()) {
					departments.fromIdentifier(api.departments.id.identifierOf(it))
				},
				singleUsePassword = result.singleUsePassword,
			)
		}
	}
		.cachedInMemory(scope.coroutineContext.job)
		.expireAfter(30.minutes, scope)

	override suspend fun list(includeClosed: Boolean): Outcome<User.Failures.List, List<User.Ref>> = out {
		client.http.request(
			api.users.get,
			api.users.idOf(),
			Unit,
			UserDto.GetParams().apply { this.includeClosed = includeClosed },
			Unit,
		).mapFailure {
			when (it.type) {
				SpineFailure.Type.Unauthenticated -> User.Failures.Unauthenticated
				SpineFailure.Type.Unauthorized -> User.Failures.Unauthorized
				else -> error("Received an unexpected status: $it")
			}
		}.bind()
			.map { fromIdentifier(api.users.id.identifierOf(it)) }
	}

	override suspend fun create(
		email: Email,
		fullName: String,
		administrator: Boolean,
	): Outcome<User.Failures.Create, Pair<User.Ref, Password>> = out {
		client.http.request(
			api.users.create,
			api.users.idOf(),
			UserDto.New(email = email.value, name = fullName, administrator = administrator),
			Parameters.Empty,
			Unit
		).mapFailure {
			when (it.type) {
				SpineFailure.Type.Unauthenticated -> User.Failures.Unauthenticated
				SpineFailure.Type.Unauthorized -> User.Failures.Unauthorized
				SpineFailure.Type.InvalidRequest -> User.Failures.UserAlreadyExists(email)
				else -> error("Received an unexpected status: $it")
			}
		}
			.bind()
			.let { fromIdentifier(api.users.id.identifierOf(it.id)) to Password(it.value) }
	}

	override suspend fun logIn(email: Email, password: Password): Outcome<User.Failures.LogIn, Pair<User.Ref, Token>> = out {
		client.http.request(
			api.users.logIn,
			api.users.idOf(),
			UserDto.LogIn(email = email.value, password = password.value),
			Parameters.Empty,
			Unit
		).mapFailure {
			when (it.type) {
				SpineFailure.Type.InvalidRequest -> User.Failures.IncorrectCredentials
				else -> error("Received an unexpected status: $it")
			}
		}.bind()
			.let { fromIdentifier(api.users.id.identifierOf(it.id)) to Token(it.value) }
	}

	override fun fromIdentifier(identifier: Identifier) = Ref(identifier.text)

	inner class Ref internal constructor(
		internal val id: String,
	) : User.Ref {
		override suspend fun join(department: Department.Ref): Outcome<User.Failures.Edit, Unit> = out {
			client.http.request(
				api.users.id.departments.add,
				api.users.id.departments.idOf(id),
				api.departments.id.idOf(department.toIdentifier().text),
				Parameters.Empty,
				Unit,
			).mapFailure {
				when (it.type) {
					SpineFailure.Type.Unauthenticated -> User.Failures.Unauthenticated
					SpineFailure.Type.Unauthorized -> User.Failures.Unauthorized
					SpineFailure.Type.NotFound -> User.Failures.NotFound(this@Ref)
					else -> error("Received an unexpected status: $it")
				}
			}.bind()

			cache.expire(this@Ref)
		}

		override suspend fun leave(department: Department.Ref): Outcome<User.Failures.Edit, Unit> = out {
			client.http.request(
				api.users.id.departments.remove,
				api.users.id.departments.idOf(id),
				api.departments.id.idOf(department.toIdentifier().text),
				Parameters.Empty,
				Unit,
			).mapFailure {
				when (it.type) {
					SpineFailure.Type.Unauthenticated -> User.Failures.Unauthenticated
					SpineFailure.Type.Unauthorized -> User.Failures.Unauthorized
					SpineFailure.Type.NotFound -> User.Failures.NotFound(this@Ref)
					else -> error("Received an unexpected status: $it")
				}
			}.bind()

			cache.expire(this@Ref)
		}

		private suspend fun edit(active: Boolean? = null, administrator: Boolean? = null): Outcome<User.Failures.SecurityEdit, Unit> = out {
			client.http.request(
				api.users.id.edit,
				api.users.id.idOf(id),
				UserDto.Edit(active = active, administrator = administrator),
				Parameters.Empty,
				Unit,
			).mapFailure {
				when (it.type) {
					SpineFailure.Type.Unauthenticated -> User.Failures.Unauthenticated
					SpineFailure.Type.Unauthorized -> User.Failures.Unauthorized
					SpineFailure.Type.NotFound -> User.Failures.NotFound(this@Ref)
					SpineFailure.Type.InvalidRequest -> User.Failures.CannotEditYourself
					else -> error("Received an unexpected status: $it")
				}
			}.bind()

			cache.expire(this@Ref)
		}

		override suspend fun enable(): Outcome<User.Failures.SecurityEdit, Unit> = edit(active = true)

		override suspend fun disable(): Outcome<User.Failures.SecurityEdit, Unit> = edit(active = false)

		override suspend fun promote(): Outcome<User.Failures.SecurityEdit, Unit> = edit(administrator = true)

		override suspend fun demote(): Outcome<User.Failures.SecurityEdit, Unit> = edit(administrator = false)

		override suspend fun resetPassword(): Outcome<User.Failures.Edit, Password> = out {
			client.http.request(
				api.users.id.password.reset,
				api.users.id.password.idOf(id),
				Unit,
				Parameters.Empty,
				Unit,
			).mapFailure {
				when (it.type) {
					SpineFailure.Type.Unauthenticated -> User.Failures.Unauthenticated
					SpineFailure.Type.Unauthorized -> User.Failures.Unauthorized
					SpineFailure.Type.NotFound -> User.Failures.NotFound(this@Ref)
					else -> error("Received an unexpected status: $it")
				}
			}.bind()
				.let { Password(it) }
				.also { cache.expire(this@Ref) }
		}

		override suspend fun setPassword(oldPassword: String, newPassword: Password): Outcome<User.Failures.SetPassword, Unit> = out {
			client.http.request(
				api.users.id.password.set,
				api.users.id.password.idOf(id),
				UserDto.SetPassword(oldPassword = oldPassword, newPassword = newPassword.value),
				Parameters.Empty,
				Unit,
			).mapFailure {
				when (it.type) {
					SpineFailure.Type.Unauthenticated -> User.Failures.Unauthenticated
					SpineFailure.Type.NotFound -> User.Failures.NotFound(this@Ref)
					SpineFailure.Type.InvalidRequest -> User.Failures.CanOnlySetYourOwnPassword
					else -> error("Received an unexpected status: $it")
				}
			}.bind()
				.also { cache.expire(this@Ref) }
		}

		override suspend fun verifyToken(token: Token): Outcome<User.Failures.TokenVerification, Unit> = out {
			client.http.request(
				api.users.id.token.verify,
				api.users.id.token.idOf(id),
				token.value,
				Parameters.Empty,
				Unit,
			).mapFailure {
				when (it.type) {
					SpineFailure.Type.InvalidRequest -> User.Failures.IncorrectCredentials
					else -> error("Received an unexpected status: $it")
				}
			}.bind()
		}

		override suspend fun logOut(token: Token): Outcome<User.Failures.Get, Unit> = out {
			client.http.request(
				api.users.id.token.logOut,
				api.users.id.token.idOf(id),
				token.value,
				Parameters.Empty,
				Unit,
			).mapFailure {
				when (it.type) {
					SpineFailure.Type.Unauthenticated -> User.Failures.Unauthenticated
					SpineFailure.Type.Unauthorized -> User.Failures.Unauthorized
					SpineFailure.Type.NotFound -> User.Failures.NotFound(this@Ref)
					else -> error("Received an unexpected status: $it")
				}
			}.bind()
		}

		override fun request(): ProgressiveFlow<User.Failures.Get, User> = flow {
			emitAll(cache[this@Ref, currentRole()])
		}

		// region Overrides

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (other !is Ref) return false

			return id == other.id
		}

		override fun hashCode(): Int {
			return id.hashCode()
		}

		override fun toString() = "RemoteUsers.Ref($id)"

		override fun toIdentifier() = Identifier(id)

		// endregion
	}
}
