package opensavvy.formulaide.test.utils

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import opensavvy.formulaide.core.Auth
import opensavvy.formulaide.core.Department
import opensavvy.formulaide.core.User
import opensavvy.formulaide.core.data.Email
import opensavvy.formulaide.core.data.Email.Companion.asEmail
import opensavvy.formulaide.core.data.Password
import opensavvy.formulaide.core.data.Token
import opensavvy.state.arrow.out
import opensavvy.state.coroutines.ProgressiveFlow
import opensavvy.state.outcome.Outcome
import opensavvy.state.progressive.success

object TestUsers : User.Service<TestUsers.Ref> {

	// region Administrator account

	val administrator = User(
		"administrator@formulaide".asEmail(),
		"Test Administrator",
		active = true,
		administrator = true,
		departments = emptySet(),
		singleUsePassword = false,
	)

	val administratorAuth = Auth(
		User.Role.Administrator,
		Ref("fake-0"),
	)

	//endregion
	//region Employee account

	val employee = User(
		"employee@formulaide".asEmail(),
		"Test Employee",
		active = true,
		administrator = false,
		departments = emptySet(),
		singleUsePassword = false,
	)

	val employeeAuth = Auth(
		User.Role.Employee,
		Ref("fake-1"),
	)

	// endregion

	override suspend fun list(includeClosed: Boolean): Outcome<User.Failures.List, List<User.Ref>> = out {
		listOfNotNull(administratorAuth.user, employeeAuth.user)
	}

	override suspend fun create(email: Email, fullName: String, administrator: Boolean): Outcome<User.Failures.Create, Pair<User.Ref, Password>> {
		error(
			"""
			TestUsers does not allow creating users. Called with:
				email: $email
				fullName: $fullName
				administrator: $administrator
			""".trimIndent()
		)
	}

	override suspend fun logIn(email: Email, password: Password): Outcome<User.Failures.LogIn, Pair<User.Ref, Token>> {
		error(
			"""
			TestUsers does not allow logging in. Called with:
				email: $email
				password: $password
			""".trimIndent()
		)
	}

	class Ref internal constructor(
		private val id: String,
	) : User.Ref {
		override suspend fun join(department: Department.Ref): Outcome<User.Failures.Edit, Unit> {
			error(
				"""
			TestUsers does not allow joining a department. Called with:
				user: $this
				department: $department
			""".trimIndent()
			)
		}

		override suspend fun leave(department: Department.Ref): Outcome<User.Failures.Edit, Unit> {
			error(
				"""
			TestUsers does not allow leaving a department. Called with:
				user: $this
				department: $department
			""".trimIndent()
			)
		}

		override suspend fun enable(): Outcome<User.Failures.SecurityEdit, Unit> {
			error(
				"""
			TestUsers does not allow enabling. Called with:
				user: $this
			""".trimIndent()
			)
		}

		override suspend fun disable(): Outcome<User.Failures.SecurityEdit, Unit> {
			error(
				"""
			TestUsers does not allow disabling. Called with:
				user: $this
			""".trimIndent()
			)
		}

		override suspend fun promote(): Outcome<User.Failures.SecurityEdit, Unit> {
			error(
				"""
			TestUsers does not allow promotion. Called with:
				user: $this
			""".trimIndent()
			)
		}

		override suspend fun demote(): Outcome<User.Failures.SecurityEdit, Unit> {
			error(
				"""
			TestUsers does not allow demotion. Called with:
				user: $this
			""".trimIndent()
			)
		}

		override suspend fun resetPassword(): Outcome<User.Failures.Edit, Password> {
			error(
				"""
			TestUsers does not allow password resetting. Called with:
				user: $this
			""".trimIndent()
			)
		}

		override suspend fun setPassword(oldPassword: String, newPassword: Password): Outcome<User.Failures.SetPassword, Unit> {
			error(
				"""
			TestUsers does not allow password edition. Called with:
				user: $this
				oldPassword: $oldPassword
				newPassword: $newPassword
			""".trimIndent()
			)
		}

		override suspend fun verifyToken(token: Token): Outcome<User.Failures.TokenVerification, Unit> {
			error(
				"""
			TestUsers does not allow token verification. Called with:
				user: $this
				token: $token
			""".trimIndent()
			)
		}

		override suspend fun logOut(token: Token): Outcome<User.Failures.Get, Unit> {
			error(
				"""
			TestUsers does not allow logging out. Called with:
				user: $this
				token: $token
			""".trimIndent()
			)
		}

		override fun request(): ProgressiveFlow<User.Failures.Get, User> = flowOf(
			when (id) {
				"fake-0" -> administrator
				"fake-1" -> employee
				else -> error("Impossible, no other instances of Ref should exist")
			}
		).map { it.success() }

		// region Overrides

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (other !is Ref) return false

			return id == other.id
		}

		override fun hashCode(): Int {
			return id.hashCode()
		}

		override fun toString() = "TestUsers.Ref($id)"

		// endregion
	}
}
