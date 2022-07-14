package formulaide.client.bones

import formulaide.api.bones.*
import formulaide.client.Client
import formulaide.core.Department
import formulaide.core.User
import formulaide.core.UserBackbone
import io.ktor.client.request.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import opensavvy.backbone.*

data class UserRef(
	val email: String,
	override val backbone: Backbone<User>,
) : Ref<User>

class Users(
	private val client: Client,
	override val cache: Cache<User>,
) : UserBackbone {
	override suspend fun all(includeClosed: Boolean): List<Ref<User>> {
		val result: List<String> = client.get("/api/users") {
			parameter("closed", includeClosed)
		}

		return result.map { UserRef(it, this) }
	}

	override suspend fun me(): Ref<User> {
		val result: ApiUser = client.get("/api/users/me")

		val ref = UserRef(result.email, this)
		val user = User(
			result.email,
			result.fullName,
			result.departments.map { DepartmentRef(it, client.departments) }.toSet(),
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
		departments: Set<Ref<Department>>,
		administrator: Boolean,
		password: String,
	) {
		val user = ApiNewUser(
			email,
			fullName,
			departments.map {
				require(it is DepartmentRef) { "$this doesn't support the reference $it" }
				it.id
			}.toSet(),
			administrator,
			password
		)

		client.post<String>("/api/users/create", user)
	}

	override suspend fun edit(
		user: Ref<User>,
		open: Boolean?,
		administrator: Boolean?,
		departments: Set<Ref<Department>>?,
	): User {
		require(user is UserRef) { "$this doesn't support the reference $user" }

		val result: ApiUser = client.patch(
			"/api/users/${user.email}", ApiUserEdition(
				open,
				administrator,
				departments?.map {
					require(it is DepartmentRef) { "$this doesn't support the reference $it" }
					it.id
				}?.toSet()
			)
		)

		return User(
			result.email,
			result.fullName,
			result.departments.map { DepartmentRef(it, client.departments) }.toSet(),
			result.administrator,
			open = result.enabled,
		).also { cache.update(user, it) }
	}

	override suspend fun setPassword(user: Ref<User>, oldPassword: String?, newPassword: String) {
		require(user is UserRef) { "$this doesn't support the reference $user" }

		client.patch<String>(
			"/api/users/${user.email}/password", ApiUserPasswordEdition(
				oldPassword,
				newPassword,
			)
		)
	}

	override fun directRequest(ref: Ref<User>): Flow<Data<User>> = flow {
		require(ref is UserRef) { "$this doesn't support the reference $ref" }

		val result: ApiUser = client.get("/api/users/${ref.email}")
		val user = User(
			result.email,
			result.fullName,
			result.departments.map { DepartmentRef(it, client.departments) }.toSet(),
			result.administrator,
			open = result.enabled,
		)

		emit(Data(Result.Success(user), Data.Status.Completed, ref))
	}
}
