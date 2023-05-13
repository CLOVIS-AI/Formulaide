package opensavvy.formulaide.fake.spies

import opensavvy.formulaide.core.Department
import opensavvy.formulaide.core.User
import opensavvy.formulaide.core.data.Email
import opensavvy.formulaide.core.data.Password
import opensavvy.formulaide.core.data.Token
import opensavvy.formulaide.core.utils.Identifier
import opensavvy.logger.loggerFor
import opensavvy.state.coroutines.ProgressiveFlow
import opensavvy.state.outcome.Outcome
import opensavvy.state.outcome.map

class SpyUsers<U : User.Ref>(private val upstream: User.Service<U>) : User.Service<SpyUsers<U>.Ref> {
	private val log = loggerFor(upstream)

	override suspend fun list(includeClosed: Boolean): Outcome<User.Failures.List, List<Ref>> = spy(
		log, "list", includeClosed,
	) { upstream.list(includeClosed) }
		.map { it.map(::Ref) }

	override suspend fun create(email: Email, fullName: String, administrator: Boolean): Outcome<User.Failures.Create, Pair<Ref, Password>> = spy(
		log, "create", email, fullName, administrator,
	) { upstream.create(email, fullName, administrator) }
		.map { (user, password) -> Ref(user) to password }

	override suspend fun logIn(email: Email, password: Password): Outcome<User.Failures.LogIn, Pair<Ref, Token>> = spy(
		log, "logIn", email, password,
	) { upstream.logIn(email, password) }
		.map { (user, token) -> Ref(user) to token }

	override fun fromIdentifier(identifier: Identifier) = upstream.fromIdentifier(identifier).let(::Ref)

	inner class Ref internal constructor(
		private val upstream: User.Ref,
	) : User.Ref {
		override suspend fun join(department: Department.Ref): Outcome<User.Failures.Edit, Unit> = spy(
			log, "join", department,
		) { upstream.join(department) }

		override suspend fun leave(department: Department.Ref): Outcome<User.Failures.Edit, Unit> = spy(
			log, "leave", department,
		) { upstream.leave(department) }

		override suspend fun enable(): Outcome<User.Failures.SecurityEdit, Unit> = spy(
			log, "enable",
		) { upstream.enable() }

		override suspend fun disable(): Outcome<User.Failures.SecurityEdit, Unit> = spy(
			log, "disable",
		) { upstream.disable() }

		override suspend fun promote(): Outcome<User.Failures.SecurityEdit, Unit> = spy(
			log, "promote",
		) { upstream.promote() }

		override suspend fun demote(): Outcome<User.Failures.SecurityEdit, Unit> = spy(
			log, "demote",
		) { upstream.demote() }

		override suspend fun resetPassword(): Outcome<User.Failures.Edit, Password> = spy(
			log, "resetPassword",
		) { upstream.resetPassword() }

		override suspend fun setPassword(oldPassword: String, newPassword: Password): Outcome<User.Failures.SetPassword, Unit> = spy(
			log, "setPassword", oldPassword, newPassword,
		) { upstream.setPassword(oldPassword, newPassword) }

		override suspend fun verifyToken(token: Token): Outcome<User.Failures.TokenVerification, Unit> = spy(
			log, "verifyToken", token,
		) { upstream.verifyToken(token) }

		override suspend fun logOut(token: Token): Outcome<User.Failures.Get, Unit> = spy(
			log, "logOut", token,
		) { upstream.logOut(token) }

		override fun request(): ProgressiveFlow<User.Failures.Get, User> = spy(
			log, "request",
		) { upstream.request() }

		// region Overrides

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (other !is SpyUsers<*>.Ref) return false

			return upstream == other.upstream
		}

		override fun hashCode(): Int {
			return upstream.hashCode()
		}

		override fun toString() = upstream.toString()
		override fun toIdentifier() = upstream.toIdentifier()

		// endregion
	}

	companion object {

		fun <U : User.Ref> User.Service<U>.spied() = SpyUsers(this)

	}
}
