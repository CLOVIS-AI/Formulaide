package opensavvy.formulaide.backend

import kotlinx.coroutines.withContext
import opensavvy.backbone.now
import opensavvy.formulaide.backend.DefaultUsers.log
import opensavvy.formulaide.core.Auth
import opensavvy.formulaide.core.Department
import opensavvy.formulaide.core.User
import opensavvy.formulaide.core.data.Email
import opensavvy.formulaide.core.data.Password
import opensavvy.formulaide.core.data.Token
import opensavvy.formulaide.core.utils.Identifier
import opensavvy.logger.Logger.Companion.info
import opensavvy.logger.Logger.Companion.warn
import opensavvy.logger.loggerFor
import opensavvy.state.arrow.out
import opensavvy.state.coroutines.ProgressiveFlow
import opensavvy.state.outcome.Outcome
import opensavvy.state.outcome.mapFailure

private object DefaultUsers {
	val log = loggerFor(this)
}

// Fake implementation used to create the initial users.
// It should NEVER be used for anything else than the initial setup of the database.
private object DefaultAdmin : User.Ref {
	override suspend fun join(department: Department.Ref): Outcome<User.Failures.Edit, Unit> {
		error("The default admin cannot join departments")
	}

	override suspend fun leave(department: Department.Ref): Outcome<User.Failures.Edit, Unit> {
		error("The default admin cannot leave departments")
	}

	override suspend fun edit(active: Boolean?, administrator: Boolean?): Outcome<User.Failures.SecurityEdit, Unit> {
		error("The default admin cannot be edited")
	}

	override suspend fun resetPassword(): Outcome<User.Failures.Edit, Password> {
		error("The default admin's password cannot be reset")
	}

	override suspend fun setPassword(oldPassword: String, newPassword: Password): Outcome<User.Failures.SetPassword, Unit> {
		error("The default admin's password cannot be set")
	}

	override suspend fun verifyToken(token: Token): Outcome<User.Failures.TokenVerification, Unit> {
		error("The default admin doesn't have tokens to verify")
	}

	override suspend fun logOut(token: Token): Outcome<User.Failures.Get, Unit> {
		error("The default admin cannot log in, and thus cannot log out either")
	}

	override fun request(): ProgressiveFlow<User.Failures.Get, User> {
		error("The default admin doesn't have information to get")
	}

	override fun toIdentifier(): Identifier {
		error("The default admin doesn't have an identifier")
	}

}

private fun fakeAdmin() = Auth(
	User.Role.Administrator,
	DefaultAdmin,
)

/**
 * This method impersonates an administrator to get a user. It should almost never be used!
 */
internal suspend fun getUserUnsafe(users: User.Service<*>, user: User.Ref) = withContext(fakeAdmin()) {
	user.now()
}

private suspend fun createUser(users: User.Service<*>, name: String, isAdmin: Boolean, email: Email, password: Password) =
	out {
		val user = users.list(includeClosed = true).bind()
			.map { it.now().bind() }
			.firstOrNull { it.email == email }

		if (user == null) {
			// The user does not exist, let's create it
			log.info { "The user $email does not exist, creating it now…" }

			val (ref, singleUsePassword) = users.create(email, name, administrator = isAdmin).bind()

			// Only a user can set their own password, so let's impersonate the user
			withContext(Auth(User.Role.Employee, ref)) {
				ref.setPassword(singleUsePassword.value, password).bind()
			}

			log.info(ref.now()) { "Done creating user $email." }
			log.warn { "The default user which was just created is an easy target for attackers. Don't forget to deactivate it when you are done creating your own account." }
		} else {
			log.info(user) { "The user $email already exists." }

			if (user.active) {
				log.warn { "The default user $email is active! This is unsafe, please deactivate it after creating your own account." }
			}
		}
	}.mapFailure {
		log.warn(it) { "Could not create user $email" }
		it
	}

suspend fun createDefaultUsers(users: User.Service<*>) = withContext(fakeAdmin()) {
	createUser(
		users,
		"Administrateur par défaut [DÉSACTIVER EN PRODUCTION]",
		isAdmin = true,
		Email("admin@formulaide"),
		Password("admin-development-password")
	)
	createUser(
		users,
		"Employé par défaut [DÉSACTIVER EN PRODUCTION]",
		isAdmin = false,
		Email("employee@formulaide"),
		Password("employee-development-password")
	)
}
