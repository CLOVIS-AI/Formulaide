package opensavvy.formulaide.api.client

import kotlinx.coroutines.currentCoroutineContext
import opensavvy.state.firstResultOrThrow

suspend fun testGuest() = Client("http://localhost:8000", currentCoroutineContext())

suspend fun testEmployee() = testGuest().apply {
	users.logIn("employee@formulaide", "employee-development-password")
		.firstResultOrThrow()
}

suspend fun testAdministrator() = testGuest().apply {
	users.logIn("admin@formulaide", "admin-development-password")
		.firstResultOrThrow()
}
