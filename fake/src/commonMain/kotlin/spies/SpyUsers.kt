package opensavvy.formulaide.fake.spies

import opensavvy.backbone.Ref
import opensavvy.backbone.RefCache
import opensavvy.formulaide.core.Department
import opensavvy.formulaide.core.User
import opensavvy.formulaide.core.data.Email
import opensavvy.formulaide.core.data.Password
import opensavvy.formulaide.core.data.Token
import opensavvy.logger.loggerFor
import opensavvy.state.outcome.Outcome

class SpyUsers(private val upstream: User.Service) : User.Service {
	private val log = loggerFor(upstream)

	override suspend fun list(includeClosed: Boolean): Outcome<List<User.Ref>> = spy(
		log, "list", includeClosed
	) { upstream.list(includeClosed) }

	override suspend fun create(
		email: Email,
		fullName: String,
		administrator: Boolean,
	): Outcome<Pair<User.Ref, Password>> = spy(
		log, "create", email, fullName, administrator,
	) { upstream.create(email, fullName, administrator) }

	override suspend fun join(user: User.Ref, department: Department.Ref): Outcome<Unit> = spy(
		log, "join", user, department,
	) { upstream.join(user, department) }

	override suspend fun leave(user: User.Ref, department: Department.Ref): Outcome<Unit> = spy(
		log, "leave", user, department,
	) { upstream.leave(user, department) }

	override suspend fun edit(user: User.Ref, active: Boolean?, administrator: Boolean?): Outcome<Unit> = spy(
		log, "edit", user, active, administrator,
	) { upstream.edit(user, active, administrator) }

	override suspend fun resetPassword(user: User.Ref): Outcome<Password> = spy(
		log, "resetPassword", user,
	) { upstream.resetPassword(user) }

	override suspend fun setPassword(user: User.Ref, oldPassword: String, newPassword: Password): Outcome<Unit> = spy(
		log, "setPassword", user, oldPassword, newPassword,
	) { upstream.setPassword(user, oldPassword, newPassword) }

	override suspend fun verifyToken(user: User.Ref, token: Token): Outcome<Unit> = spy(
		log, "verifyToken", user, token,
	) { upstream.verifyToken(user, token) }

	override suspend fun logIn(email: Email, password: Password): Outcome<Pair<User.Ref, Token>> = spy(
		log, "logIn", email, password,
	) { upstream.logIn(email, password) }

	override suspend fun logOut(user: User.Ref, token: Token): Outcome<Unit> = spy(
		log, "logOut", user, token,
	) { upstream.logOut(user, token) }

	override val cache: RefCache<User>
		get() = upstream.cache

	override suspend fun directRequest(ref: Ref<User>): Outcome<User> = spy(
		log, "directRequest", ref,
	) { upstream.directRequest(ref) }

	companion object {

		fun User.Service.spied() = SpyUsers(this)

	}
}
