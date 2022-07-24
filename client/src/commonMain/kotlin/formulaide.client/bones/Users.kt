package formulaide.client.bones

import formulaide.api.bones.ApiNewUser
import formulaide.api.bones.ApiPasswordLogin
import formulaide.api.bones.ApiUserEdition
import formulaide.api.bones.ApiUserPasswordEdition
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
	override suspend fun all(includeClosed: Boolean): List<User.Ref> {
		val result: List<User.Ref> = client.get("/api/users") {
			parameter("closed", includeClosed)
		}

		return result
	}

	override suspend fun me(): User.Ref {
		val result: User = client.get("/api/users/me")

		val ref = User.Ref(result.email, this)
		val user = User(
			result.email,
			result.fullName,
			result.departments,
			result.administrator,
			open = result.open,
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
		departments: Set<Department.Ref>,
		administrator: Boolean,
		password: String,
	): User.Ref {
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
		user: User.Ref,
		open: Boolean?,
		administrator: Boolean?,
		departments: Set<Department.Ref>?,
	) {
		client.patch<String>(
			"/api/users/${user.email}", ApiUserEdition(
				open,
				administrator,
				departments
			)
		)
		user.expire()
	}

	override suspend fun setPassword(user: User.Ref, oldPassword: String?, newPassword: String) {
		client.patch<String>(
			"/api/users/${user.email}/password", ApiUserPasswordEdition(
				oldPassword,
				newPassword,
			)
		)
	}

	override fun directRequest(ref: Ref<User>): Flow<Data<User>> = flow {
		require(ref is User.Ref) { "$this doesn't support the reference $ref" }

		val result: User = client.get("/api/users/${ref.email}")
		val user = User(
			result.email,
			result.fullName,
			result.departments,
			result.administrator,
			open = result.open,
		)

		emit(Data(Result.Success(user), Data.Status.Completed, ref))
	}
}
