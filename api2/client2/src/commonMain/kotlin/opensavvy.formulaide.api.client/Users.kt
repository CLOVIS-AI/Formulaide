package opensavvy.formulaide.api.client

import io.ktor.http.*
import opensavvy.backbone.Ref
import opensavvy.backbone.Ref.Companion.expire
import opensavvy.backbone.RefCache
import opensavvy.formulaide.api.Context
import opensavvy.formulaide.api.User
import opensavvy.formulaide.api.api2
import opensavvy.formulaide.core.AbstractUsers
import opensavvy.formulaide.core.Department
import opensavvy.spine.Parameters
import opensavvy.spine.ktor.client.request
import opensavvy.state.slice.Slice
import opensavvy.state.slice.ensureValid
import opensavvy.state.slice.slice
import opensavvy.formulaide.core.User as CoreUser

class Users(
	private val client: Client,
	override val cache: RefCache<CoreUser>,
) : AbstractUsers {
	override suspend fun list(includeClosed: Boolean): Slice<List<CoreUser.Ref>> = slice {
		val parameters = User.GetParams().apply {
			this.includeClosed = includeClosed
		}

		val list = client.http
			.request(api2.users.get, api2.users.idOf(), Unit, parameters, client.context.value)
			.bind()

		list.map {
			CoreUser.Ref(api2.users.id.idFrom(it, client.context.value).bind(), this@Users)
		}
	}

	/**
	 * Finds the ID of the currently logged-in user.
	 *
	 * Only employees or higher can call this function.
	 */
	suspend fun me(): Slice<CoreUser.Ref> = slice {
		val result = client.http
			.request(api2.users.me.get, api2.users.me.idOf(), Unit, Parameters.Empty, client.context.value)
			.bind()

		CoreUser.Ref(api2.users.id.idFrom(result, client.context.value).bind(), this@Users)
	}

	/**
	 * Attempts to log in as the user with the [email] and [password].
	 *
	 * If the password is a single-use password (see [CoreUser.forceResetPassword]), it is consumed and will not be usable a second time.
	 */
	suspend fun logIn(email: String, password: String): Slice<Unit> = slice {
		val body = User.LogInForm(
			email = email,
			password = password,
		)

		val (id, user) = client.http
			.request(
				api2.users.me.logIn,
				api2.users.me.idOf(),
				body,
				Parameters.Empty,
				client.context.value,
				onResponse = { response ->
					// Find the cookie in the response and store it in RAM.
					// When using the official frontend, the cookie is HttpOnly and thus invisible from our code
					// During automated testing or when running on the JVM, the cookie is found
					response.setCookie().find { it.name == "session" }
						?.value
						?.let { client.token = it }
				}
			).bind()

		val ref = CoreUser.Ref(api2.users.id.idFrom(id, client.context.value).bind(), this@Users)
		client.context.value = Context(
			ref,
			if (user.administrator) CoreUser.Role.ADMINISTRATOR else CoreUser.Role.EMPLOYEE
		)
	}

	/**
	 * Logs out and invalidates the current token.
	 */
	suspend fun logOut(): Slice<Unit> = slice {
		client.http
			.request(api2.users.me.logOut, api2.users.me.idOf(), Unit, Parameters.Empty, client.context.value)
			.bind()

		client.users.cache.expireAll()
		client.departments.cache.expireAll()
		client.forms.cache.expireAll()
		client.formVersions.cache.expireAll()
		client.templates.cache.expireAll()
		client.templateVersions.cache.expireAll()
		client.token = null
		client.context.value = Context(user = null, role = CoreUser.Role.ANONYMOUS)
	}

	override suspend fun create(
		email: String,
		fullName: String,
		departments: Set<Department.Ref>,
		administrator: Boolean,
	): Slice<Pair<CoreUser.Ref, String>> = slice {
		val body = User.New(
			email = email,
			name = fullName,
			departments = departments.mapTo(HashSet()) { api2.departments.id.idOf(it.id) },
			administrator = administrator,
		)

		val (id, password) = client.http
			.request(api2.users.create, api2.users.idOf(), body, Parameters.Empty, client.context.value)
			.bind()

		CoreUser.Ref(
			api2.users.id.idFrom(id, client.context.value).bind(),
			this@Users
		) to password.singleUsePassword
	}

	override suspend fun edit(
		user: CoreUser.Ref,
		open: Boolean?,
		administrator: Boolean?,
		departments: Set<Department.Ref>?,
	): Slice<Unit> = slice {
		val body = User.Edit(
			open = open,
			administrator = administrator,
			departments = departments?.mapTo(HashSet()) { api2.departments.id.idOf(it.id) },
		)

		client.http
			.request(api2.users.id.edit, api2.users.id.idOf(user.id), body, Parameters.Empty, client.context.value)
			.bind()

		user.expire()
	}

	suspend fun setPassword(
		oldPassword: String,
		newPassword: String,
	): Slice<Unit> = slice {
		val body = User.PasswordModification(
			oldPassword = oldPassword,
			newPassword = newPassword,
		)

		client.http
			.request(api2.users.me.editPassword, api2.users.me.idOf(), body, Parameters.Empty, client.context.value)
			.bind()
	}

	override suspend fun resetPassword(user: CoreUser.Ref): Slice<String> = slice {
		client.http
			.request(
				api2.users.id.resetPassword,
				api2.users.id.idOf(user.id),
				Unit,
				Parameters.Empty,
				client.context.value
			).bind()
			.singleUsePassword
	}

	override suspend fun directRequest(ref: Ref<CoreUser>): Slice<CoreUser> = slice {
		ensureValid(ref is CoreUser.Ref) { "${this@Users} n'accepte pas la référence $ref" }

		val user = client.http
			.request(api2.users.id.get, api2.users.id.idOf(ref.id), Unit, Parameters.Empty, client.context.value)
			.bind()

		CoreUser(
			email = user.email,
			name = user.name,
			open = user.open,
			departments = user.departments.mapTo(HashSet()) {
				Department.Ref(
					api2.departments.id.idFrom(
						it,
						client.context.value
					).bind(),
					client.departments
				)
			},
			administrator = user.administrator,
			forceResetPassword = user.singleUsePassword,
		)
	}
}
