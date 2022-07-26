package formulaide.client.bones

import formulaide.api.bones.*
import formulaide.client.Client
import formulaide.core.Department
import formulaide.core.User
import formulaide.core.UserBackbone
import io.ktor.client.request.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import opensavvy.backbone.Cache
import opensavvy.backbone.Data
import opensavvy.backbone.Ref
import opensavvy.backbone.Ref.Companion.expire
import opensavvy.backbone.Result

class Users(
	private val client: Client,
	override val cache: Cache<User>,
) : UserBackbone {
	override suspend fun all(includeClosed: Boolean): List<formulaide.core.Ref<User>> {
		val result: List<formulaide.core.Ref<User>> = client.get("/api/users") {
			parameter("closed", includeClosed)
		}

		return result
	}

	override suspend fun me(): formulaide.core.Ref<User> {
		val result: ApiUser = client.get("/api/users/me")

		val ref = formulaide.core.Ref(result.email, this)
		val user = User(
			result.email,
			result.fullName,
			result.departments.map { formulaide.core.Ref(it.toString(), client.departments) }.toSet(),
			result.administrator,
			open = result.enabled,
		)

		cache.update(ref, user)

		return ref
	}

	override suspend fun logIn(email: String, password: String): String =
		client.post<String>("/api/auth/login", ApiPasswordLogin(email, password))
			.removeSurrounding("\"")

	override suspend fun create(
		email: String,
		fullName: String,
		departments: Set<formulaide.core.Ref<Department>>,
		administrator: Boolean,
		password: String,
	): formulaide.core.Ref<User> {
		val user = ApiNewUser(
			email,
			fullName,
			departments,
			administrator,
			password
		)

		return client.post("/api/users/create", user)
	}

	override suspend fun edit(
		user: formulaide.core.Ref<User>,
		open: Boolean?,
		administrator: Boolean?,
		departments: Set<formulaide.core.Ref<Department>>?,
	) {
		client.patch<String>(
			"/api/users/${user.id}", ApiUserEdition(
				open,
				administrator,
				departments
			)
		)
		user.expire()
	}

	override suspend fun setPassword(user: formulaide.core.Ref<User>, oldPassword: String?, newPassword: String) {
		client.patch<String>(
			"/api/users/${user.id}/password", ApiUserPasswordEdition(
				oldPassword,
				newPassword,
			)
		)
	}

	override fun directRequest(ref: Ref<User>): Flow<Data<User>> = flow {
		require(ref is formulaide.core.Ref) { "$this doesn't support the reference $ref" }

		val result: ApiUser = client.get("/api/users/${ref.id}")
		val user = User(
			result.email,
			result.fullName,
			result.departments,
			result.administrator,
			open = result.enabled,
		)

		emit(Data(Result.Success(user), Data.Status.Completed, ref))
	}
}
