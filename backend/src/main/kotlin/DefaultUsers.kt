package opensavvy.formulaide.backend

import arrow.core.getOrHandle
import kotlinx.coroutines.withContext
import opensavvy.backbone.Ref.Companion.now
import opensavvy.formulaide.backend.DefaultUsers.log
import opensavvy.formulaide.core.Auth
import opensavvy.formulaide.core.User
import opensavvy.formulaide.core.data.Email
import opensavvy.formulaide.core.data.Password
import opensavvy.logger.Logger.Companion.info
import opensavvy.logger.Logger.Companion.warn
import opensavvy.logger.loggerFor
import opensavvy.state.outcome.out

private object DefaultUsers {
	val log = loggerFor(this)
}

private fun fakeAdmin(users: User.Service) = Auth(
	User.Role.Administrator,
	User.Ref(
		"this is the fake admin required to create the initial accounts, it should never appear anywhere else",
		users
	)
)

private suspend fun createUser(users: User.Service, name: String, isAdmin: Boolean, email: Email, password: Password) =
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
	}.getOrHandle {
		log.warn(it) { "Could not create user $email" }
	}

suspend fun createDefaultUsers(users: User.Service) = withContext(fakeAdmin(users)) {
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
