package formulaide.client.routes

import formulaide.api.types.Email
import formulaide.api.types.Ref
import formulaide.api.users.*
import formulaide.client.Client
import formulaide.client.bones.UserRef
import opensavvy.backbone.Ref.Companion.requestValue

private fun formulaide.core.User.toLegacy() = User(
	Email(email),
	fullName,
	departments.map {
		require(it is formulaide.core.Ref) { "$this doesn't support the reference $it" }
		Ref<Service>(it.id)
	}.toSet(),
	administrator,
	open,
)

/**
 * Gets a [TokenResponse] from the server, from a [PasswordLogin].
 *
 * > This method is part of the legacy API.
 * > It will be deprecated in the future.
 * > See [Client.departments]
 */
suspend fun Client.login(passwordLogin: PasswordLogin): TokenResponse =
	TokenResponse(users.logIn(passwordLogin.email, passwordLogin.password))

/**
 * Creates a new user.
 *
 * > This method is part of the legacy API.
 * > It will be deprecated in the future.
 * > See [Client.departments]
 */
suspend fun Client.Authenticated.createUser(newUser: NewUser) {
	users.create(
		newUser.user.email.email,
		newUser.user.fullName,
		newUser.user.services.map { formulaide.core.Ref(it.id, departments) }.toSet(),
		newUser.user.administrator,
		newUser.password,
	)
}

/**
 * Gets the current user's data.
 *
 * > This method is part of the legacy API.
 * > It will be deprecated in the future.
 * > See [Client.departments]
 */
suspend fun Client.Authenticated.getMe(): User =
	users.me().requestValue().toLegacy()

/**
 * Edits a [user]'s information.
 *
 * > This method is part of the legacy API.
 * > It will be deprecated in the future.
 * > See [Client.departments]
 */
suspend fun Client.Authenticated.editUser(
	user: User,
	enabled: Boolean? = null,
	administrator: Boolean? = null,
	services: Set<Ref<Service>> = emptySet(),
): User {
	val ref = UserRef(user.id, users)
	users.edit(ref, enabled, administrator, services.map { formulaide.core.Ref(it.id, departments) }.toSet())
	return ref.requestValue().toLegacy()
}

/**
 * Edits a user's password.
 *
 * > This method is part of the legacy API.
 * > It will be deprecated in the future.
 * > See [Client.departments]
 */
suspend fun Client.Authenticated.editPassword(edit: PasswordEdit) {
	val ref = UserRef(edit.user.email, users)
	users.setPassword(ref, edit.oldPassword, edit.newPassword)
}

/**
 * Gets the list of [users][User].
 *
 * > This method is part of the legacy API.
 * > It will be deprecated in the future.
 * > See [Client.departments]
 */
suspend fun Client.Authenticated.listUsers(evenDisabled: Boolean = false): List<User> =
	users.all(evenDisabled)
		.map { it.requestValue() }
		.map { it.toLegacy() }
