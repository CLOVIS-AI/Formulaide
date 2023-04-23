package opensavvy.formulaide.fake

import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import opensavvy.cache.contextual.cache
import opensavvy.formulaide.core.*
import opensavvy.formulaide.core.data.Email
import opensavvy.formulaide.core.data.Password
import opensavvy.formulaide.core.data.Token
import opensavvy.formulaide.fake.utils.newId
import opensavvy.logger.Logger.Companion.warn
import opensavvy.logger.loggerFor
import opensavvy.state.arrow.out
import opensavvy.state.coroutines.ProgressiveFlow
import opensavvy.state.outcome.Outcome
import kotlin.random.Random
import kotlin.random.nextUInt

class FakeUsers : User.Service<FakeUsers.Ref> {

	private val log = loggerFor(this)

	private val users = HashMap<Long, User>()
	private val passwords = HashMap<Long, Password>()
	private val tokens = HashMap<Long, ArrayList<Token>>()
	private val blocked = HashSet<Long>()

	private val lock = Semaphore(1)

	private val cache = cache<Ref, User.Role, User.Failures.Get, User> { ref, role ->
		out {
			ensureEmployee { User.Failures.Unauthenticated }

			lock.withPermit {
				val user = users[ref.id]
				ensureNotNull(user) { User.Failures.NotFound(ref) }
				ensure(user.active || role >= User.Role.Administrator) {
					log.warn(ref) { "User ${currentUser()} attempted to access user $ref. Only administrators can access other users." }
					User.Failures.NotFound(ref)
				}
				user
			}
		}
	}

	override suspend fun list(includeClosed: Boolean): Outcome<User.Failures.List, List<User.Ref>> = out {
		ensureEmployee { User.Failures.Unauthenticated }
		ensureAdministrator { User.Failures.Unauthorized }

		lock.withPermit {
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

		lock.withPermit {
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

		lock.withPermit {
			users[id] = user
			passwords[id] = singleUsePassword
		}

		Ref(id) to singleUsePassword
	}

	override suspend fun logIn(email: Email, password: Password): Outcome<User.Failures.LogIn, Pair<User.Ref, Token>> = out {
		lock.withPermit {
			val (id, user) = users.asSequence()
				.firstOrNull { it.value.email == email }
				?: raise(User.Failures.IncorrectCredentials())

			ensure(user.active) {
				log.warn(email) { "Blocked login because the user is inactive" }
				User.Failures.IncorrectCredentials()
			}

			ensure(passwords[id] == password) {
				log.warn(email) { "Blocked login because the password is incorrect" }
				User.Failures.IncorrectCredentials()
			}

			ensure(id !in blocked) {
				log.warn(email) { "Blocked login because the user is blocked" }
				User.Failures.IncorrectCredentials()
			}

			val token = Token("very-strong-token-${Random.nextUInt()}")
			tokens.getOrPut(id) { ArrayList() }
				.add(token)

			if (user.singleUsePassword)
				blocked += id

			Ref(id) to token
		}
	}

	inner class Ref internal constructor(
		val id: Long,
	) : User.Ref {

		private suspend fun edit(transform: (User) -> User): Outcome<User.Failures.Edit, Unit> = out {
			ensureEmployee { User.Failures.Unauthenticated }
			ensureAdministrator { User.Failures.Unauthorized }

			lock.withPermit {
				val current = users[id]
				ensureNotNull(current) { User.Failures.NotFound(this@Ref) }

				users[id] = transform(current)
			}

			cache.expire(this@Ref)
		}

		override suspend fun join(department: Department.Ref): Outcome<User.Failures.Edit, Unit> =
			edit { it.copy(departments = it.departments + department) }

		override suspend fun leave(department: Department.Ref): Outcome<User.Failures.Edit, Unit> =
			edit { it.copy(departments = it.departments - department) }

		private suspend fun securityEdit(transform: (User) -> User): Outcome<User.Failures.SecurityEdit, Unit> = out {
			ensureEmployee { User.Failures.Unauthenticated }
			ensureAdministrator { User.Failures.Unauthorized }

			ensure(this@Ref != currentUser()) { User.Failures.CannotEditYourself }

			lock.withPermit {
				val current = users[id]
				ensureNotNull(current) { User.Failures.NotFound(this@Ref) }

				users[id] = transform(current)
			}

			cache.expire(this@Ref)
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

			lock.withPermit {
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

			lock.withPermit {
				val password = passwords[id]
				ensure(oldPassword == password?.value) { User.Failures.IncorrectPassword() }

				passwords[id] = newPassword
				tokens[id]?.clear()

				users[id]?.copy(singleUsePassword = false)
					?.also { users[id] = it }

				blocked.remove(id)
			}
		}

		override suspend fun verifyToken(token: Token): Outcome<User.Failures.TokenVerification, Unit> = out {
			lock.withPermit {
				val theirs = tokens[id]
				ensureNotNull(theirs) {
					log.warn(this@Ref) { "Could not find any tokens for user" }
					User.Failures.IncorrectCredentials()
				}
				ensure(token in theirs) {
					log.warn(this@Ref) { "The provided token is invalid" }
					User.Failures.IncorrectCredentials()
				}
			}
		}

		override suspend fun logOut(token: Token): Outcome<User.Failures.Get, Unit> = out {
			ensureEmployee { User.Failures.Unauthenticated }
			ensure(this@Ref == currentUser()) {
				log.warn { "'${currentUser()}' attempted to log out $this@Ref" }
				User.Failures.Unauthorized
			}

			lock.withPermit {
				tokens[id]?.remove(token)
			}
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

		override fun toString() = "FakeUsers.Ref($id)"

		// endregion
	}
}
