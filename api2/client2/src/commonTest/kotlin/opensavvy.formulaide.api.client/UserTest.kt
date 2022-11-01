@file:OptIn(ExperimentalCoroutinesApi::class)

package opensavvy.formulaide.api.client

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.test.runTest
import opensavvy.logger.Logger.Companion.info
import opensavvy.logger.loggerFor
import opensavvy.state.slice.valueOrThrow
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class UserTest {

	private val log = loggerFor(this)

	@Test
	fun create() = runTest {
		val admin = testAdministrator()

		log.info { "Creating a user…" }

		val number = Random.nextInt()
		val (user, tmpPassword) = admin.users
			.create("email.$number@opensavvy.dev", "User client-side creation test", emptySet(), administrator = false)
			.valueOrThrow

		log.info { "Logging in as that user…" }

		val employee = testGuest()

		employee.users
			.logIn("email.$number@opensavvy.dev", tmpPassword)
			.valueOrThrow

		assertEquals(user.id, employee.users.me().valueOrThrow.id)

		log.info { "Editing my password…" }

		employee.users.setPassword(tmpPassword, "789456123")
			.valueOrThrow

		log.info { "Log out…" }

		employee.users.logOut()
			.valueOrThrow

		log.info { "Closing the user…" }

		admin.users.edit(user, open = false)
			.valueOrThrow

		currentCoroutineContext().cancelChildren()
	}

}
