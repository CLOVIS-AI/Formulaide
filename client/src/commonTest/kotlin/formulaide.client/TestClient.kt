package formulaide.client

import formulaide.api.types.Email
import formulaide.api.users.NewUser
import formulaide.api.users.PasswordLogin
import formulaide.api.users.User
import formulaide.client.routes.createUser
import formulaide.client.routes.listUsers
import formulaide.client.routes.login
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

fun testClient() = Client.Anonymous.connect("http://localhost:8000")

@OptIn(DelicateCoroutinesApi::class)
private val testEmployeeMemo = GlobalScope.launch {
	val admin = testAdministrator()

	val existingEmployee = admin.listUsers()
		.find { it.email.email == "employee@formulaide" }

	if (existingEmployee == null)
		admin.createUser(
			NewUser(
				"employee-development-password",
				User(Email("employee@formulaide"), "Employé", admin.me.services, false)
			)
		)
}

suspend fun testEmployee(): Client.Authenticated {
	testEmployeeMemo.join()

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
