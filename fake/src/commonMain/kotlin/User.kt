package opensavvy.formulaide.fake

import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import opensavvy.backbone.Ref
import opensavvy.backbone.RefCache
import opensavvy.backbone.defaultRefCache
import opensavvy.formulaide.core.Auth.Companion.currentRole
import opensavvy.formulaide.core.Auth.Companion.currentUser
import opensavvy.formulaide.core.Auth.Companion.ensureAdministrator
import opensavvy.formulaide.core.Auth.Companion.ensureEmployee
import opensavvy.formulaide.core.Department
import opensavvy.formulaide.core.User
import opensavvy.formulaide.core.data.Email
import opensavvy.formulaide.core.data.Password
import opensavvy.formulaide.core.data.Token
import opensavvy.formulaide.fake.utils.newId
import opensavvy.state.outcome.*
import kotlin.random.Random
import kotlin.random.nextUInt

class FakeUsers : User.Service {

	override val cache: RefCache<User> = defaultRefCache()

	private val users = HashMap<String, User>()
	private val passwords = HashMap<String, Password>()
	private val tokens = HashMap<String, ArrayList<Token>>()
	private val blocked = HashSet<String>()

	private val lock = Semaphore(1)

	private fun toRef(id: String) = User.Ref(id, this)

	override suspend fun list(includeClosed: Boolean): Outcome<List<User.Ref>> = out {
		ensureAdministrator()

		lock.withPermit {
			users.asSequence()
				.filter { (_, it) -> it.active || includeClosed }
				.map { toRef(it.key) }
				.toList()
		}
	}

	override suspend fun create(
		email: Email,
		fullName: String,
		administrator: Boolean,
	): Outcome<Pair<User.Ref, Password>> = out {
		ensureAdministrator()

		lock.withPermit {
			ensureValid(email !in users.values.map { it.email }) { "The email $email already belongs to another user" }
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

		toRef(id) to singleUsePassword
	}

	override suspend fun join(user: User.Ref, department: Department.Ref): Outcome<Unit> = out {
		ensureAdministrator()

		lock.withPermit {
			val current = users[user.id]!!

			users[user.id] = current.copy(departments = current.departments + department)
		}
	}

	override suspend fun leave(user: User.Ref, department: Department.Ref): Outcome<Unit> = out {
		ensureAdministrator()

		lock.withPermit {
			val current = users[user.id]!!

			users[user.id] = current.copy(departments = current.departments - department)
		}
	}

	override suspend fun edit(user: User.Ref, active: Boolean?, administrator: Boolean?): Outcome<Unit> = out {
		ensureAdministrator()

		if (user == currentUser()) {
			ensureValid(active == null) { "Cannot edit your own activity status" }
			ensureValid(administrator == null) { "Cannot edit your own role" }
		}

		lock.withPermit {
			val current = users[user.id]!!

			users[user.id] = current.copy(
				active = active ?: current.active,
				administrator = administrator ?: current.administrator,
			)
		}
	}

	override suspend fun resetPassword(user: User.Ref): Outcome<Password> = out {
		ensureAdministrator()

		val newPassword = Password("some-new-password-${Random.nextUInt()}")

		lock.withPermit {
			val current = users[user.id]
			ensureFound(current != null) { "Could not find user $user" }

			passwords[user.id] = newPassword
			tokens[user.id]?.clear()

			users[user.id] = current.copy(singleUsePassword = true)
			blocked.remove(user.id)
		}

		newPassword
	}

	override suspend fun setPassword(user: User.Ref, oldPassword: String, newPassword: Password): Outcome<Unit> = out {
		ensureEmployee()
		ensureAuthorized(user == currentUser()) { "Setting the password of another user is forbidden" }

		lock.withPermit {
			val password = passwords[user.id]
			ensureValid(oldPassword == password?.value) { "The wrong password was provided" }

			passwords[user.id] = newPassword
			tokens[user.id]?.clear()

			users[user.id]?.copy(singleUsePassword = false)
				?.also { users[user.id] = it }

			blocked.remove(user.id)
		}
	}

	override suspend fun verifyToken(user: User.Ref, token: Token): Outcome<Unit> = out {
		lock.withPermit {
			val theirs = tokens[user.id]
			ensureFound(theirs != null) { "User $user doesn't exist" }
			ensureAuthenticated(token in theirs) { "The provided token is invalid" }
		}
	}

	override suspend fun logIn(email: Email, password: Password): Outcome<Pair<User.Ref, Token>> = out {
		lock.withPermit {
			val (id, user) = users.asSequence().first { it.value.email == email }
			ensureFound(user.active) { "Could not find user $email" }
			ensureAuthenticated(passwords[id] == password) { "Incorrect password" }
			ensureAuthenticated(id !in blocked) { "The single-use password has already been used." }

			val token = Token("very-strong-token-${Random.nextUInt()}")
			tokens.getOrPut(id) { ArrayList() }
				.add(token)

			if (user.singleUsePassword)
				blocked += id

			toRef(id) to token
		}
	}

	override suspend fun logOut(user: User.Ref, token: Token): Outcome<Unit> = out {
		ensureAuthenticated(currentUser() == user) { "Cannot log out another user" }

		lock.withPermit {
			tokens[user.id]?.remove(token)
		}
	}

	override suspend fun directRequest(ref: Ref<User>): Outcome<User> = out {
		ensureEmployee()
		ensureValid(ref is User.Ref) { "Wrong ref: $ref" }

		lock.withPermit {
			val user = users[ref.id]
			ensureFound(user != null) { "Could not find user $ref" }
			ensureFound(user.active || currentRole() >= User.Role.Administrator) { "Could not find user $user" }
			user
		}
	}

}
