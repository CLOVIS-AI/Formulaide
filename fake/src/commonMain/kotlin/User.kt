package opensavvy.formulaide.fake

import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import opensavvy.formulaide.core.*
import opensavvy.formulaide.core.data.Email
import opensavvy.formulaide.core.data.Password
import opensavvy.formulaide.core.data.Token
import opensavvy.formulaide.core.utils.Identifier
import opensavvy.formulaide.fake.utils.newId
import opensavvy.logger.Logger.Companion.warn
import opensavvy.logger.loggerFor
import opensavvy.state.arrow.out
import opensavvy.state.coroutines.ProgressiveFlow
import opensavvy.state.outcome.Outcome
import opensavvy.state.progressive.withProgress
import kotlin.random.Random
import kotlin.random.nextUInt

class FakeUsers : User.Service<FakeUsers.Ref> {

	private val log = loggerFor(this)

	private val users = HashMap<Long, User>()
	private val passwords = HashMap<Long, Password>()
	private val tokens = HashMap<Long, ArrayList<Token>>()
	private val blocked = HashSet<Long>()

	private val lock = Mutex()

	override suspend fun list(includeClosed: Boolean): Outcome<User.Failures.List, List<User.Ref>> = out {
		ensureEmployee { User.Failures.Unauthenticated }
		ensureAdministrator { User.Failures.Unauthorized }

		lock.withLock("list") {
			users.asSequence()
				.filter { (_, it) -> it.active || includeClosed }
				.map { Ref(it.key) }
				.toList()
		}
	}

	override suspend fun create(
		email: Email,
		fullName: String,
		administrator: Boolean,
	): Outcome<User.Failures.Create, Pair<User.Ref, Password>> = out {
		ensureEmployee { User.Failures.Unauthenticated }
		ensureAdministrator { User.Failures.Unauthorized }

		lock.withLock("create:initial") {
			ensure(email !in users.values.map { it.email }) { User.Failures.UserAlreadyExists(email) }
		}

		val id = newId()

		val singleUsePassword = Password("generated-single-use-password-${Random.nextUInt()}")
		val user = User(
			email = email,
			name = fullName,
			active = true,
			administrator = administrator,
			departments = emptySet(),
			singleUsePassword = true,
		)

		lock.withLock("create:final") {
			users[id] = user
			passwords[id] = singleUsePassword
		}

		Ref(id) to singleUsePassword
	}

	override suspend fun logIn(email: Email, password: Password): Outcome<User.Failures.LogIn, Pair<User.Ref, Token>> = out {
		lock.withLock("logIn") {
			val (id, user) = users.asSequence()
				.firstOrNull { it.value.email == email }
				?: raise(User.Failures.IncorrectCredentials)

			ensure(user.active) {
				log.warn(email) { "Blocked login because the user is inactive" }
				User.Failures.IncorrectCredentials
			}

			ensure(passwords[id] == password) {
				log.warn(email) { "Blocked login because the password is incorrect" }
				User.Failures.IncorrectCredentials
			}

			ensure(id !in blocked) {
				log.warn(email) { "Blocked login because the user is blocked" }
				User.Failures.IncorrectCredentials
			}

			val token = Token("very-strong-token-${Random.nextUInt()}")
			tokens.getOrPut(id) { ArrayList() }
				.add(token)

			if (user.singleUsePassword)
				blocked += id

			Ref(id) to token
		}
	}

	override fun fromIdentifier(identifier: Identifier) = Ref(identifier.text.toLong())

	inner class Ref internal constructor(
		val id: Long,
	) : User.Ref {

		private suspend fun edit(transform: (User) -> User): Outcome<User.Failures.Edit, Unit> = out {
			ensureEmployee { User.Failures.Unauthenticated }
			ensureAdministrator { User.Failures.Unauthorized }

			lock.withLock("edit") {
				val current = users[id]
				ensureNotNull(current) { User.Failures.NotFound(this@Ref) }

				users[id] = transform(current)
			}
		}

		override suspend fun join(department: Department.Ref): Outcome<User.Failures.Edit, Unit> =
			edit { it.copy(departments = it.departments + department) }

		override suspend fun leave(department: Department.Ref): Outcome<User.Failures.Edit, Unit> =
			edit { it.copy(departments = it.departments - department) }

		private suspend fun securityEdit(transform: (User) -> User): Outcome<User.Failures.SecurityEdit, Unit> = out {
			ensureEmployee { User.Failures.Unauthenticated }
			ensureAdministrator { User.Failures.Unauthorized }

			ensure(this@Ref != currentUser()) { User.Failures.CannotEditYourself }

			lock.withLock("securityEdit") {
				val current = users[id]
				ensureNotNull(current) { User.Failures.NotFound(this@Ref) }

				users[id] = transform(current)
			}
		}

		override suspend fun edit(active: Boolean?, administrator: Boolean?): Outcome<User.Failures.SecurityEdit, Unit> =
			securityEdit {
				var result = it

				if (active != null)
					result = result.copy(active = active)

				if (administrator != null)
					result = result.copy(administrator = administrator)

				result
			}

		override suspend fun enable(): Outcome<User.Failures.SecurityEdit, Unit> =
			securityEdit { it.copy(active = true) }

		override suspend fun disable(): Outcome<User.Failures.SecurityEdit, Unit> =
			securityEdit { it.copy(active = false) }

		override suspend fun promote(): Outcome<User.Failures.SecurityEdit, Unit> =
			securityEdit { it.copy(administrator = true) }

		override suspend fun demote(): Outcome<User.Failures.SecurityEdit, Unit> =
			securityEdit { it.copy(administrator = false) }

		override suspend fun resetPassword(): Outcome<User.Failures.Edit, Password> = out {
			ensureEmployee { User.Failures.Unauthenticated }
			ensureAdministrator { User.Failures.Unauthorized }

			val newPassword = Password("some-new-password-${Random.nextUInt()}")

			lock.withLock("resetPassword") {
				val current = users[id]
				ensureNotNull(current) { User.Failures.NotFound(this@Ref) }

				passwords[id] = newPassword
				tokens[id]?.clear()

				users[id] = current.copy(singleUsePassword = true)
				blocked.remove(id)
			}

			newPassword
		}

		override suspend fun setPassword(oldPassword: String, newPassword: Password): Outcome<User.Failures.SetPassword, Unit> = out {
			ensureEmployee { User.Failures.Unauthenticated }
			ensure(this@Ref == currentUser()) { User.Failures.CanOnlySetYourOwnPassword }

			lock.withLock("setPassword") {
				val password = passwords[id]
				ensure(oldPassword == password?.value) { User.Failures.IncorrectPassword }

				passwords[id] = newPassword
				tokens[id]?.clear()

				users[id]?.copy(singleUsePassword = false)
					?.also { users[id] = it }

				blocked.remove(id)
			}
		}

		override suspend fun verifyToken(token: Token): Outcome<User.Failures.TokenVerification, Unit> = out {
			lock.withLock("verifyToken") {
				val theirs = tokens[id]
				ensureNotNull(theirs) {
					log.warn(this@Ref) { "Could not find any tokens for user" }
					User.Failures.IncorrectCredentials
				}
				ensure(token in theirs) {
					log.warn(this@Ref) { "The provided token is invalid" }
					User.Failures.IncorrectCredentials
				}
			}
		}

		override suspend fun logOut(token: Token): Outcome<User.Failures.Get, Unit> = out {
			ensureEmployee { User.Failures.Unauthenticated }
			ensure(this@Ref == currentUser()) {
				log.warn { "'${currentUser()}' attempted to log out $this@Ref" }
				User.Failures.Unauthorized
			}

			lock.withLock("logOut") {
				tokens[id]?.remove(token)
			}
		}

		override fun request(): ProgressiveFlow<User.Failures.Get, User> = flow {
			out {
				ensureEmployee { User.Failures.Unauthenticated }

				lock.withLock("request") {
					val user = users[id]
					ensureNotNull(user) { User.Failures.NotFound(this@Ref) }
					ensure(user.active || currentRole() >= User.Role.Administrator) {
						log.warn(this@Ref) { "User ${currentUser()} attempted to access user ${this@Ref}. Only administrators can access other users." }
						User.Failures.NotFound(this@Ref)
					}
					user
				}
			}.also { emit(it.withProgress()) }
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

		override fun toString() = "FakeUsers.Ref($id)"
		override fun toIdentifier() = Identifier(id.toString())

		// endregion
	}
}
