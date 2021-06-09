package formulaide.client.routes

import formulaide.api.users.NewUser
import formulaide.api.users.PasswordLogin
import formulaide.api.users.TokenResponse
import formulaide.api.users.User
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
