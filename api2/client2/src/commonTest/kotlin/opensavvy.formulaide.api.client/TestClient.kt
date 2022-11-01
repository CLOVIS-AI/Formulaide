package opensavvy.formulaide.api.client

import kotlinx.coroutines.currentCoroutineContext
import opensavvy.state.slice.valueOrThrow

suspend fun testGuest() = Client("http://localhost:8000", currentCoroutineContext())

suspend fun testEmployee() = testGuest().apply {
	users.logIn("employee@formulaide", "employee-development-password").valueOrThrow
}

suspend fun testAdministrator() = testGuest().apply {
	users.logIn("admin@formulaide", "admin-development-password").valueOrThrow
}
