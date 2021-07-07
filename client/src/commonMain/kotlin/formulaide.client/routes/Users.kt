package formulaide.client.routes

import formulaide.api.users.*
import formulaide.client.Client

/**
 * Gets a [TokenResponse] from the server, from a [PasswordLogin].
 *
 * - POST /users/login
 * - Body: [PasswordLogin]
 * - Response: [TokenResponse]
 */
suspend fun Client.login(passwordLogin: PasswordLogin) =
	post<TokenResponse>("/users/login", body = passwordLogin)

/**
 * Creates a new user.
 *
 * - POST /users/create
 * - Requires 'administrator' right
 * - Body: [NewUser]
 * - Response: [TokenResponse]
 */
suspend fun Client.Authenticated.createUser(newUser: NewUser) =
	post<TokenResponse>("/users/create", body = newUser)

/**
 * Gets the current user's data.
 *
 * - GET /users/me
 * - Requires authentication
 * - Response: [User]
 */
suspend fun Client.Authenticated.getMe() =
	get<User>("/users/me")

/**
 * Edits a [user]'s information.
 *
 * See [UserEdits] for an explanation of the parameters.
 *
 * - POST /users/edit
 * - Requires 'administrator' rights
 * - Body: [UserEdits]
 * - Response: [User]
 */
suspend fun Client.Authenticated.editUser(
	user: User,
	enabled: Boolean? = null,
	administrator: Boolean? = null,
) =
	post<User>("/users/edit", body = UserEdits(user.email, enabled, administrator))

/**
 * Edits a user's password.
 *
 * - POST /users/password
 * - Requires 'employee' rights
 * - Body: [PasswordEdit]
 * - Response: a success message
 */
suspend fun Client.Authenticated.editPassword(edit: PasswordEdit) =
	post<String>("/users/password", body = edit)

/**
 * Gets the list of [users][User].
 *
 * - GET /users/listEnabled
 * - GET /users/listAll
 * - Response: list of [User]
 *
 * @param evenDisabled If `true`, users that are disabled are queried as well.
 */
suspend fun Client.Authenticated.listUsers(evenDisabled: Boolean = false) =
	get<List<User>>(if (!evenDisabled) "/users/listEnabled" else "/users/listAll")
