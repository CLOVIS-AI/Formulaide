package opensavvy.formulaide.test.utils

import opensavvy.backbone.Ref
import opensavvy.backbone.RefCache
import opensavvy.backbone.defaultRefCache
import opensavvy.formulaide.core.Auth
import opensavvy.formulaide.core.Department
import opensavvy.formulaide.core.User
import opensavvy.formulaide.core.data.Email
import opensavvy.formulaide.core.data.Email.Companion.asEmail
import opensavvy.formulaide.core.data.Password
import opensavvy.formulaide.core.data.Token
import opensavvy.state.outcome.Outcome
import opensavvy.state.outcome.ensureValid
import opensavvy.state.outcome.out

object TestUsers : User.Service {

	//region Administrator account

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
		User.Ref("fake-0", this),
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
		User.Ref("fake-1", this),
	)

	//endregion

	override suspend fun list(includeClosed: Boolean): Outcome<List<User.Ref>> = out {
		listOfNotNull(administratorAuth.user, employeeAuth.user)
	}

	override suspend fun create(
		email: Email,
		fullName: String,
		administrator: Boolean,
	): Outcome<Pair<User.Ref, Password>> {
		error(
			"""
			TestUsers does not allow creating new users. Called with:
				email: $email
				fullName: '$fullName'
				administrator: $administrator
			""".trimIndent()
		)
	}

	override suspend fun join(user: User.Ref, department: Department.Ref): Outcome<Unit> {
		error(
			"""
			TestUsers does not allow joining a department. Called with:
				user: $user
				department: $department
			""".trimIndent()
		)
	}

	override suspend fun leave(user: User.Ref, department: Department.Ref): Outcome<Unit> {
		error(
			"""
			TestUsers does not allow leaving a department. Called with:
				user: $user
				department: $department
			""".trimIndent()
		)
	}

	override suspend fun edit(user: User.Ref, active: Boolean?, administrator: Boolean?): Outcome<Unit> {
		error(
			"""
			TestUsers does not allow edition. Called with:
				user: $user
				active: $active
				administrator: $administrator
			""".trimIndent()
		)
	}

	override suspend fun resetPassword(user: User.Ref): Outcome<Password> {
		error(
			"""
			TestUsers does not allow resetting a password. Called with:
				user: $user
			""".trimIndent()
		)
	}

	override suspend fun setPassword(user: User.Ref, oldPassword: String, newPassword: Password): Outcome<Unit> {
		error(
			"""
			TestUsers does not allow setting a password. Called with:
				user: $user
				oldPassword: $oldPassword
				newPassword: $newPassword
			""".trimIndent()
		)
	}

	override suspend fun verifyToken(user: User.Ref, token: Token): Outcome<Unit> {
		error(
			"""
			TestUsers does not allow verifying tokens. Called with:
				user: $user
				token: $token
			""".trimIndent()
		)
	}

	override suspend fun logIn(email: Email, password: Password): Outcome<Pair<User.Ref, Token>> {
		error(
			"""
			TestUsers does not allow setting a password. Called with:
				email: $email
				password: $password
			""".trimIndent()
		)
	}

	override suspend fun logOut(user: User.Ref, token: Token): Outcome<Unit> {
		error(
			"""
			TestUsers does not allow logging out. Called with:
				user: $user
				token: $token
			""".trimIndent()
		)
	}

	override val cache: RefCache<User> = defaultRefCache()

	override suspend fun directRequest(ref: Ref<User>): Outcome<User> = out {
		ensureValid(ref is User.Ref) { "Invalid ref: $ref" }

		when (ref.id) {
			"fake:0" -> administrator
			"fake:1" -> employee
			else -> error("The provided reference does not belong to TestUsers: $ref")
		}
	}
}