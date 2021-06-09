package formulaide.client

import formulaide.api.users.PasswordLogin
import formulaide.client.routes.login

fun testClient() = Client.Anonymous.connect("http://localhost:8000")

suspend fun testEmployee(): Client.Authenticated {
	val anonymous = testClient()
	val token = anonymous.login(
		PasswordLogin("employee-development-password", "employee@formulaide")
	)
	return anonymous.authenticate(token.token)
}

suspend fun testAdministrator(): Client.Authenticated {
	val anonymous = testClient()
	val token = anonymous.login(
		PasswordLogin("admin-development-password", "admin@formulaide")
	)
	return anonymous.authenticate(token.token)
}
