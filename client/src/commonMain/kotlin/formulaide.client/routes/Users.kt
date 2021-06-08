package formulaide.client.routes

import formulaide.api.users.PasswordLogin
import formulaide.api.users.TokenResponse
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
