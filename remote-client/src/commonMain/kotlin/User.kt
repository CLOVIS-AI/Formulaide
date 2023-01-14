package opensavvy.formulaide.remote.client

import opensavvy.backbone.Ref
import opensavvy.backbone.Ref.Companion.expire
import opensavvy.backbone.RefCache
import opensavvy.backbone.defaultRefCache
import opensavvy.cache.ExpirationCache.Companion.expireAfter
import opensavvy.cache.MemoryCache.Companion.cachedInMemory
import opensavvy.formulaide.core.Department
import opensavvy.formulaide.core.User
import opensavvy.formulaide.core.data.Email
import opensavvy.formulaide.core.data.Password
import opensavvy.formulaide.core.data.Token
import opensavvy.formulaide.remote.api
import opensavvy.formulaide.remote.dto.UserDto
import opensavvy.spine.Parameters
import opensavvy.spine.ktor.client.request
import opensavvy.state.outcome.Outcome
import opensavvy.state.outcome.ensureValid
import opensavvy.state.outcome.out
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.minutes

class Users(
	private val client: Client,
	cacheContext: CoroutineContext,
	private val departments: Department.Service,
) : User.Service {

	override val cache: RefCache<User> = defaultRefCache<User>()
		.cachedInMemory(cacheContext)
		.expireAfter(30.minutes, cacheContext)

	override suspend fun list(includeClosed: Boolean): Outcome<List<User.Ref>> = out {
		client.http.request(
			api.users.get,
			api.users.idOf(),
			Unit,
			UserDto.GetParams().apply { this.includeClosed = includeClosed },
			Unit,
		).bind()
			.map { api.users.id.refOf(it, this@Users).bind() }
	}

	override suspend fun create(
		email: Email,
		fullName: String,
		administrator: Boolean,
	): Outcome<Pair<User.Ref, Password>> = out {
		client.http.request(
			api.users.create,
			api.users.idOf(),
			UserDto.New(email = email.value, name = fullName, administrator = administrator),
			Parameters.Empty,
			Unit
		)
			.bind()
			.let { api.users.id.refOf(it.id, this@Users).bind() to Password(it.value) }
	}

	override suspend fun join(user: User.Ref, department: Department.Ref): Outcome<Unit> = out {
		client.http.request(
			api.users.id.departments.add,
			api.users.id.departments.idOf(user.id),
			api.departments.id.idOf(department.id),
			Parameters.Empty,
			Unit,
		).bind()
			.also { user.expire() }
	}

	override suspend fun leave(user: User.Ref, department: Department.Ref): Outcome<Unit> = out {
		client.http.request(
			api.users.id.departments.remove,
			api.users.id.departments.idOf(user.id),
			api.departments.id.idOf(department.id),
			Parameters.Empty,
			Unit,
		).bind()
			.also { user.expire() }
	}

	override suspend fun edit(user: User.Ref, active: Boolean?, administrator: Boolean?): Outcome<Unit> = out {
		client.http.request(
			api.users.id.edit,
			api.users.id.idOf(user.id),
			UserDto.Edit(active = active, administrator = administrator),
			Parameters.Empty,
			Unit,
		).bind()
			.also { user.expire() }
	}

	override suspend fun resetPassword(user: User.Ref): Outcome<Password> = out {
		client.http.request(
			api.users.id.password.reset,
			api.users.id.password.idOf(user.id),
			Unit,
			Parameters.Empty,
			Unit,
		).bind()
			.let { Password(it) }
			.also { user.expire() }
	}

	override suspend fun setPassword(user: User.Ref, oldPassword: String, newPassword: Password): Outcome<Unit> = out {
		client.http.request(
			api.users.id.password.set,
			api.users.id.password.idOf(user.id),
			UserDto.SetPassword(oldPassword = oldPassword, newPassword = newPassword.value),
			Parameters.Empty,
			Unit,
		).bind()
			.also { user.expire() }
	}

	override suspend fun verifyToken(user: User.Ref, token: Token): Outcome<Unit> = out {
		client.http.request(
			api.users.id.token.verify,
			api.users.id.token.idOf(user.id),
			token.value,
			Parameters.Empty,
			Unit,
		).bind()
	}

	override suspend fun logIn(email: Email, password: Password): Outcome<Pair<User.Ref, Token>> = out {
		client.http.request(
			api.users.logIn,
			api.users.idOf(),
			UserDto.LogIn(email = email.value, password = password.value),
			Parameters.Empty,
			Unit
		).bind()
			.let { api.users.id.refOf(it.id, this@Users).bind() to Token(it.value) }
	}

	override suspend fun logOut(user: User.Ref, token: Token): Outcome<Unit> = out {
		client.http.request(
			api.users.id.token.logOut,
			api.users.id.token.idOf(user.id),
			token.value,
			Parameters.Empty,
			Unit,
		).bind()
	}

	override suspend fun directRequest(ref: Ref<User>): Outcome<User> = out {
		ensureValid(ref is User.Ref) { "Expected User.Ref, found $ref" }

		val result = client.http.request(
			api.users.id.get,
			api.users.id.idOf(ref.id),
			Unit,
			Parameters.Empty,
			Unit,
		).bind()

		User(
			email = Email(result.email),
			name = result.name,
			active = result.active,
			administrator = result.administrator,
			departments = result.departments.mapTo(HashSet()) {
				api.departments.id.refOf(it, departments).bind()
			},
			singleUsePassword = result.singleUsePassword,
		)
	}

}
