@file:OptIn(ExperimentalCoroutinesApi::class)

package opensavvy.formulaide.database.document

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.test.runTest
import opensavvy.backbone.Ref.Companion.requestValue
import opensavvy.formulaide.core.User
import opensavvy.formulaide.database.testDatabase
import opensavvy.logger.Logger.Companion.debug
import opensavvy.logger.Logger.Companion.info
import opensavvy.logger.loggerFor
import opensavvy.state.Slice.Companion.failed
import opensavvy.state.Status
import opensavvy.state.firstResult
import opensavvy.state.firstResultOrThrow
import kotlin.random.Random
import kotlin.test.*

class UserTest {

	private val log = loggerFor(this)

	@Test
	fun createEmployee() = runTest {
		val database = testDatabase()

		log.info { "Creating a user…" }

		val number = Random.nextInt()
		val (user, _) = database.users
			.create("email.$number@opensavvy.dev", "User creation test", emptySet(), administrator = false)
			.firstResultOrThrow()

		log.info { "Checking values…" }

		val userData = user.requestValue()
		assertEquals("email.$number@opensavvy.dev", userData.email)
		assertEquals("User creation test", userData.name)
		assertEquals(emptySet(), userData.departments)
		assertEquals(false, userData.administrator)
		assertEquals(User.Role.EMPLOYEE, userData.role)

		log.info { "Closing…" }

		database.users.edit(user, open = false).firstResultOrThrow()

		currentCoroutineContext().cancelChildren()
	}

	@Test
	fun createAdmin() = runTest {
		val database = testDatabase()

		log.info { "Creating a user…" }

		val number = Random.nextInt()
		val (user, _) = database.users
			.create("email.$number@opensavvy.dev", "User creation test", emptySet(), administrator = true)
			.firstResultOrThrow()

		log.info { "Checking values…" }

		val userData = user.requestValue()
		assertEquals("email.$number@opensavvy.dev", userData.email)
		assertEquals("User creation test", userData.name)
		assertEquals(emptySet(), userData.departments)
		assertEquals(true, userData.administrator)
		assertEquals(User.Role.ADMINISTRATOR, userData.role)

		log.info { "Closing…" }

		database.users.edit(user, open = false).firstResultOrThrow()

		currentCoroutineContext().cancelChildren()
	}

	@Test
	fun close() = runTest {
		val database = testDatabase()

		log.info { "Creating a user…" }

		val number = Random.nextInt()
		val (user, password) = database.users
			.create("email.$number@opensavvy.dev", "User closing test", emptySet(), administrator = false)
			.firstResultOrThrow()

		log.info { "Checking that it appears as an open user…" }

		val openUsers = database.users.list(includeClosed = false)
			.firstResultOrThrow()
		assertContains(openUsers, user)

		log.info { "Checking its values" }

		assertTrue(user.requestValue().open)

		log.info { "Closing…" }

		database.users.edit(user, open = false).firstResultOrThrow()

		log.info { "Checking that it doesn't appear as an open user anymore…" }

		val openUsers2 = database.users.list(includeClosed = false)
			.firstResultOrThrow()
		assertFalse(user in openUsers2)

		val closedUsers = database.users.list(includeClosed = true)
			.firstResultOrThrow()
		assertContains(closedUsers, user)

		log.info { "Checking its values…" }

		assertFalse(user.requestValue().open)

		log.info { "Attempting to log in…" }

		assertFails {
			// It should be closed, so it shouldn't be possible to log in, even with the correct password
			database.users.logIn("email.$number@opensavvy.dev", password).firstResultOrThrow()
		}

		currentCoroutineContext().cancelChildren()
	}

	@Test
	fun departments() = runTest {
		val database = testDatabase()

		log.info { "Creating a user and two departments…" }

		val dep1 = database.departments.create("User department test 1")
			.firstResultOrThrow()
		val dep2 = database.departments.create("User department test 2")
			.firstResultOrThrow()

		assertNotEquals(dep1, dep2)

		val number = Random.nextInt()
		val (user, _) = database.users
			.create("email.$number@opensavvy.dev", "User department test", setOf(dep1), administrator = false)
			.firstResultOrThrow()

		log.info { "Checking values…" }

		assertEquals(setOf(dep1), user.requestValue().departments)

		log.info { "Replacing the departments…" }

		database.users.edit(user, departments = setOf(dep2)).firstResultOrThrow()
		assertEquals(setOf(dep2), user.requestValue().departments)

		log.info { "Adding both departments…" }

		database.users.edit(user, departments = setOf(dep1, dep2)).firstResultOrThrow()
		assertEquals(setOf(dep1, dep2), user.requestValue().departments)

		log.info { "Closing…" }

		database.users.edit(user, open = false).firstResultOrThrow()
		dep1.close()
		dep2.close()

		currentCoroutineContext().cancelChildren()
	}

	@Test
	fun sameEmail() = runTest {
		val database = testDatabase()

		log.info { "Creating a user…" }

		val number = Random.nextInt()
		log.debug(number) { "Email unique number" }

		val (user, _) = database.users
			.create("email.$number@opensavvy.dev", "Double email test (1)", emptySet(), administrator = false)
			.firstResultOrThrow()

		log.info { "Attempting to create another user with the same email address…" }

		val result = database.users
			.create("email.$number@opensavvy.dev", "Double email test (2)", emptySet(), administrator = false)
			.firstResult()

		assertEquals(
			failed(
				Status.StandardFailure.Kind.Invalid,
				"Un utilisateur possède déjà cette adresse électronique : 'email.$number@opensavvy.dev'"
			), result
		)

		log.info { "Closing…" }

		database.users.edit(user, open = false).firstResultOrThrow()

		currentCoroutineContext().cancelChildren()
	}

	@Test
	fun logIn() = runTest {
		val database = testDatabase()

		log.info { "Creating a user…" }

		val number = Random.nextInt()
		val (user, password) = database.users
			.create("email.$number@opensavvy.dev", "Log in user test", emptySet(), administrator = false)
			.firstResultOrThrow()
		assertTrue(user.requestValue().forceResetPassword)

		log.info { "Attempting to log in…" }

		val (_, token) = database.users.logIn("email.$number@opensavvy.dev", password).firstResultOrThrow()

		log.info { "Checking token…" }

		database.users.verifyToken(user, token).firstResultOrThrow()

		log.info { "Checking a fake token…" }

		assertFails {
			database.users.verifyToken(user, "wtf").firstResultOrThrow()
		}

		log.info { "Trying to log in again. If everything went well, the password is not valid anymore because it was already used." }

		assertFails {
			database.users.logIn("email.$number@opensavvy.dev", password).firstResultOrThrow()
		}

		log.info { "Invalidating the token…" }

		database.users.logOut(user, token).firstResultOrThrow()

		assertFails {
			database.users.verifyToken(user, token).firstResultOrThrow()
		}

		log.info { "Closing…" }

		database.users.edit(user, open = false).firstResultOrThrow()

		currentCoroutineContext().cancelChildren()
	}

	@Test
	fun passwordEdition() = runTest {
		val database = testDatabase()

		log.info { "Creating a user…" }

		val number = Random.nextInt()
		val (user, originalPassword) = database.users
			.create("email.$number@opensavvy.dev", "Log in user test", emptySet(), administrator = false)
			.firstResultOrThrow()
		assertTrue(user.requestValue().forceResetPassword)

		log.info { "Logging in…" }

		database.users.logIn("email.$number@opensavvy.dev", originalPassword).firstResultOrThrow()

		log.info { "Editing the password…" }

		database.users.setPassword(user, originalPassword, "123456789").firstResultOrThrow()
		assertFalse(user.requestValue().forceResetPassword)

		log.info { "Logging in multiple times…" }

		val (_, token1) = database.users.logIn("email.$number@opensavvy.dev", "123456789").firstResultOrThrow()
		val (_, token2) = database.users.logIn("email.$number@opensavvy.dev", "123456789").firstResultOrThrow()

		database.users.verifyToken(user, token1).firstResultOrThrow()
		database.users.verifyToken(user, token2).firstResultOrThrow()

		log.info { "Setting a password too short is forbidden" }

		assertFails { database.users.setPassword(user, "12345789", "1234").firstResultOrThrow() }

		log.info { "Editing the password" }

		database.users.setPassword(user, "123456789", "456789123").firstResultOrThrow()

		// The previous tokens should not be valid anymore…
		assertFails { database.users.verifyToken(user, token1).firstResultOrThrow() }
		assertFails { database.users.verifyToken(user, token2).firstResultOrThrow() }

		// …but it should be possible to create new ones
		val (_, token3) = database.users.logIn("email.$number@opensavvy.dev", "456789123").firstResultOrThrow()
		val (_, token4) = database.users.logIn("email.$number@opensavvy.dev", "456789123").firstResultOrThrow()

		database.users.verifyToken(user, token3).firstResultOrThrow()
		database.users.verifyToken(user, token4).firstResultOrThrow()

		log.info { "The administrator can reset the account, invalidating all tokens…" }

		val newPassword = database.users.resetPassword(user).firstResultOrThrow()
		assertTrue(user.requestValue().forceResetPassword)

		// The original tokens should of course still be invalid
		assertFails { database.users.verifyToken(user, token1).firstResultOrThrow() }
		assertFails { database.users.verifyToken(user, token2).firstResultOrThrow() }

		// The previous ones should be invalid as well
		assertFails { database.users.verifyToken(user, token3).firstResultOrThrow() }
		assertFails { database.users.verifyToken(user, token4).firstResultOrThrow() }

		// The new password can be used to log in a single time
		val (_, token5) = database.users.logIn("email.$number@opensavvy.dev", newPassword).firstResultOrThrow()
		assertFails { database.users.logIn("email.$number@opensavvy.dev", newPassword).firstResultOrThrow() }

		database.users.verifyToken(user, token5).firstResultOrThrow()

		log.info { "Closing…" }

		database.users.edit(user, open = false).firstResultOrThrow()

		currentCoroutineContext().cancelChildren()
	}
}
