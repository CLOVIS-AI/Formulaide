package opensavvy.formulaide.api.client

import kotlinx.coroutines.flow.emitAll
import opensavvy.backbone.Ref
import opensavvy.backbone.Ref.Companion.expire
import opensavvy.backbone.Ref.Companion.request
import opensavvy.backbone.RefCache
import opensavvy.formulaide.api.Context
import opensavvy.formulaide.api.User
import opensavvy.formulaide.api.api2
import opensavvy.formulaide.api.utils.bind
import opensavvy.formulaide.api.utils.flatMapSuccess
import opensavvy.formulaide.api.utils.mapSuccess
import opensavvy.formulaide.api.utils.onEachSuccess
import opensavvy.formulaide.core.AbstractUsers
import opensavvy.formulaide.core.Department
import opensavvy.spine.Parameters
import opensavvy.spine.ktor.client.request
import opensavvy.state.State
import opensavvy.state.ensureValid
import opensavvy.state.state
import opensavvy.formulaide.core.User as CoreUser

class Users(
	private val client: Client,
	override val cache: RefCache<CoreUser>,
) : AbstractUsers {
	override fun list(includeClosed: Boolean): State<List<CoreUser.Ref>> = state {
		val parameters = User.GetParams().apply {
			this.includeClosed = includeClosed
		}

		val result = client.http
			.request(api2.users.get, api2.users.idOf(), Unit, parameters, client.context)
			.mapSuccess { list ->
				list.map {
					CoreUser.Ref(bind(api2.users.id.idFrom(it, client.context)), this@Users)
				}
			}

		emitAll(result)
	}

	/**
	 * Finds the ID of the currently logged-in user.
	 *
	 * Only employees or higher can call this function.
	 */
	fun me(): State<CoreUser.Ref> = state {
		val result = client.http
			.request(api2.users.me.get, api2.users.me.idOf(), Unit, Parameters.Empty, client.context)
			.mapSuccess {
				CoreUser.Ref(bind(api2.users.id.idFrom(it, client.context)), this@Users)
			}

		emitAll(result)
	}

	/**
	 * Attempts to log in as the user with the [email] and [password].
	 *
	 * If the password is a single-use password (see [CoreUser.forceResetPassword]), it is consumed and will not be usable a second time.
	 */
	fun logIn(email: String, password: String): State<Unit> = state {
		val body = User.LogInForm(
			email = email,
			password = password,
		)

		val result = client.http
			.request(api2.users.me.logIn, api2.users.me.idOf(), body, Parameters.Empty, client.context)
			.flatMapSuccess { (id, _) ->
				val ref = CoreUser.Ref(bind(api2.users.id.idFrom(id, client.context)), this@Users)
				ref.expire()
				emitAll(ref.request().mapSuccess { ref to it })
			}
			.onEachSuccess { (ref, user) ->
				client.context = Context(ref, user.role)
			}
			.mapSuccess { }

		emitAll(result)
	}

	/**
	 * Logs out and invalidates the current token.
	 */
	fun logOut(): State<Unit> = state {
		val result = client.http
			.request(api2.users.me.logOut, api2.users.me.idOf(), Unit, Parameters.Empty, client.context)
			.onEachSuccess {
				client.users.cache.expireAll()
				client.departments.cache.expireAll()
				client.context = Context(user = null, role = CoreUser.Role.ANONYMOUS)
			}
			.mapSuccess { }

		emitAll(result)
	}

	override fun create(
		email: String,
		fullName: String,
		departments: Set<Department.Ref>,
		administrator: Boolean,
	): State<Pair<CoreUser.Ref, String>> = state {
		val body = User.New(
			email = email,
			name = fullName,
			departments = departments.mapTo(HashSet()) { api2.departments.id.idOf(it.id) },
			administrator = administrator,
		)

		val result = client.http
			.request(api2.users.create, api2.users.idOf(), body, Parameters.Empty, client.context)
			.mapSuccess { (id, password) ->
				CoreUser.Ref(bind(api2.users.id.idFrom(id, client.context)), this@Users) to password.singleUsePassword
			}

		emitAll(result)
	}

	override fun edit(
		user: CoreUser.Ref,
		open: Boolean?,
		administrator: Boolean?,
		departments: Set<Department.Ref>?,
	): State<Unit> = state {
		val body = User.Edit(
			open = open,
			administrator = administrator,
			departments = departments?.mapTo(HashSet()) { api2.departments.id.idOf(it.id) },
		)

		val result = client.http
			.request(api2.users.id.edit, api2.users.id.idOf(user.id), body, Parameters.Empty, client.context)
			.onEachSuccess { user.expire() }

		emitAll(result)
	}

	fun setPassword(
		oldPassword: String,
		newPassword: String,
	): State<Unit> = state {
		val body = User.PasswordModification(
			oldPassword = oldPassword,
			newPassword = newPassword,
		)

		val result = client.http
			.request(api2.users.me.editPassword, api2.users.me.idOf(), body, Parameters.Empty, client.context)

		emitAll(result)
	}

	override fun resetPassword(user: CoreUser.Ref): State<String> = state {
		val result = client.http
			.request(api2.users.id.resetPassword, api2.users.id.idOf(user.id), Unit, Parameters.Empty, client.context)
			.mapSuccess { it.singleUsePassword }

		emitAll(result)
	}

	override fun directRequest(ref: Ref<CoreUser>): State<CoreUser> = state {
		ensureValid(ref is CoreUser.Ref) { "${this@Users} n'accepte pas la référence $ref" }

		val result = client.http
			.request(api2.users.id.get, api2.users.id.idOf(ref.id), Unit, Parameters.Empty, client.context)
			.mapSuccess { user ->
				CoreUser(
					email = user.email,
					name = user.name,
					open = user.open,
					departments = user.departments.mapTo(HashSet()) {
						Department.Ref(
							bind(
								api2.departments.id.idFrom(
									it,
									client.context
								)
							), client.departments
						)
					},
					administrator = user.administrator,
					forceResetPassword = user.singleUsePassword,
				)
			}

		emitAll(result)
	}
}
