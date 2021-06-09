package formulaide.client

import formulaide.api.users.PasswordLogin
import formulaide.client.routes.login

fun testClient() = AnonymousClient("http://localhost:8000")

suspend fun testEmployee(): AuthenticatedClient {
	val anonymous = testClient()
	val token = anonymous.login(
		PasswordLogin("employee-development-password", "employee@formulaide")
	)
	return AuthenticatedClient(anonymous, token.token)
}

suspend fun testAdministrator(): AuthenticatedClient {
	val anonymous = testClient()
	val token = anonymous.login(
		PasswordLogin("admin-development-password", "admin@formulaide")
	)
	return AuthenticatedClient(anonymous, token.token)
}
