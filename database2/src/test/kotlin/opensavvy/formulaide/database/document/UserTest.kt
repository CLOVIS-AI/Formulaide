@file:OptIn(ExperimentalCoroutinesApi::class)

package opensavvy.formulaide.database.document

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.test.runTest
import opensavvy.backbone.Ref.Companion.requestValueOrThrow
import opensavvy.formulaide.core.User
import opensavvy.formulaide.database.testDatabase
import opensavvy.logger.Logger.Companion.debug
import opensavvy.logger.Logger.Companion.info
import opensavvy.logger.loggerFor
import opensavvy.state.Failure
import opensavvy.state.slice.failed
import opensavvy.state.slice.valueOrThrow
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
			.valueOrThrow

		log.info { "Checking values…" }

		val userData = user.requestValueOrThrow()
		assertEquals("email.$number@opensavvy.dev", userData.email)
		assertEquals("User creation test", userData.name)
		assertEquals(emptySet(), userData.departments)
		assertEquals(false, userData.administrator)
		assertEquals(User.Role.EMPLOYEE, userData.role)

		log.info { "Closing…" }

		database.users.edit(user, open = false).valueOrThrow

		currentCoroutineContext().cancelChildren()
	}

	@Test
	fun createAdmin() = runTest {
		val database = testDatabase()

		log.info { "Creating a user…" }

		val number = Random.nextInt()
		val (user, _) = database.users
			.create("email.$number@opensavvy.dev", "User creation test", emptySet(), administrator = true)
			.valueOrThrow

		log.info { "Checking values…" }

		val userData = user.requestValueOrThrow()
		assertEquals("email.$number@opensavvy.dev", userData.email)
		assertEquals("User creation test", userData.name)
		assertEquals(emptySet(), userData.departments)
		assertEquals(true, userData.administrator)
		assertEquals(User.Role.ADMINISTRATOR, userData.role)

		log.info { "Closing…" }

		database.users.edit(user, open = false).valueOrThrow

		currentCoroutineContext().cancelChildren()
	}

	@Test
	fun close() = runTest {
		val database = testDatabase()

		log.info { "Creating a user…" }

		val number = Random.nextInt()
		val (user, password) = database.users
			.create("email.$number@opensavvy.dev", "User closing test", emptySet(), administrator = false)
			.valueOrThrow

		log.info { "Checking that it appears as an open user…" }

		val openUsers = database.users.list(includeClosed = false)
			.valueOrThrow
		assertContains(openUsers, user)

		log.info { "Checking its values" }

		assertTrue(user.requestValueOrThrow().open)

		log.info { "Closing…" }

		database.users.edit(user, open = false).valueOrThrow

		log.info { "Checking that it doesn't appear as an open user anymore…" }

		val openUsers2 = database.users.list(includeClosed = false)
			.valueOrThrow
		assertFalse(user in openUsers2)

		val closedUsers = database.users.list(includeClosed = true)
			.valueOrThrow
		assertContains(closedUsers, user)

		log.info { "Checking its values…" }

		assertFalse(user.requestValueOrThrow().open)

		log.info { "Attempting to log in…" }

		assertFails {
			// It should be closed, so it shouldn't be possible to log in, even with the correct password
			database.users.logIn("email.$number@opensavvy.dev", password).valueOrThrow
		}

		currentCoroutineContext().cancelChildren()
	}

	@Test
	fun departments() = runTest {
		val database = testDatabase()

		log.info { "Creating a user and two departments…" }

		val dep1 = database.departments.create("User department test 1")
			.valueOrThrow
		val dep2 = database.departments.create("User department test 2")
			.valueOrThrow

		assertNotEquals(dep1, dep2)

		val number = Random.nextInt()
		val (user, _) = database.users
			.create("email.$number@opensavvy.dev", "User department test", setOf(dep1), administrator = false)
			.valueOrThrow

		log.info { "Checking values…" }

		assertEquals(setOf(dep1), user.requestValueOrThrow().departments)

		log.info { "Replacing the departments…" }

		database.users.edit(user, departments = setOf(dep2)).valueOrThrow
		assertEquals(setOf(dep2), user.requestValueOrThrow().departments)

		log.info { "Adding both departments…" }

		database.users.edit(user, departments = setOf(dep1, dep2)).valueOrThrow
		assertEquals(setOf(dep1, dep2), user.requestValueOrThrow().departments)

		log.info { "Closing…" }

		database.users.edit(user, open = false).valueOrThrow
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
			.valueOrThrow

		log.info { "Attempting to create another user with the same email address…" }

		val result = database.users
			.create("email.$number@opensavvy.dev", "Double email test (2)", emptySet(), administrator = false)

		assertEquals(
			failed(
				"Un utilisateur possède déjà cette adresse électronique : 'email.$number@opensavvy.dev'",
				Failure.Kind.Invalid,
			), result
		)

		log.info { "Closing…" }

		database.users.edit(user, open = false).valueOrThrow

		currentCoroutineContext().cancelChildren()
	}

	@Test
	fun logIn() = runTest {
		val database = testDatabase()

		log.info { "Creating a user…" }

		val number = Random.nextInt()
		val (user, password) = database.users
			.create("email.$number@opensavvy.dev", "Log in user test", emptySet(), administrator = false)
			.valueOrThrow
		assertTrue(user.requestValueOrThrow().forceResetPassword)

		log.info { "Attempting to log in…" }

		val (_, token) = database.users.logIn("email.$number@opensavvy.dev", password).valueOrThrow

		log.info { "Checking token…" }

		database.users.verifyToken(user, token).valueOrThrow

		log.info { "Checking a fake token…" }

		assertFails {
			database.users.verifyToken(user, "wtf").valueOrThrow
		}

		log.info { "Trying to log in again. If everything went well, the password is not valid anymore because it was already used." }

		assertFails {
			database.users.logIn("email.$number@opensavvy.dev", password).valueOrThrow
		}

		log.info { "Invalidating the token…" }

		database.users.logOut(user, token).valueOrThrow

		assertFails {
			database.users.verifyToken(user, token).valueOrThrow
		}

		log.info { "Closing…" }

		database.users.edit(user, open = false).valueOrThrow

		currentCoroutineContext().cancelChildren()
	}

	@Test
	fun passwordEdition() = runTest {
		val database = testDatabase()

		log.info { "Creating a user…" }

		val number = Random.nextInt()
		val (user, originalPassword) = database.users
			.create("email.$number@opensavvy.dev", "Log in user test", emptySet(), administrator = false)
			.valueOrThrow
		assertTrue(user.requestValueOrThrow().forceResetPassword)

		log.info { "Logging in…" }

		database.users.logIn("email.$number@opensavvy.dev", originalPassword).valueOrThrow

		log.info { "Editing the password…" }

		database.users.setPassword(user, originalPassword, "123456789").valueOrThrow
		assertFalse(user.requestValueOrThrow().forceResetPassword)

		log.info { "Logging in multiple times…" }

		val (_, token1) = database.users.logIn("email.$number@opensavvy.dev", "123456789").valueOrThrow
		val (_, token2) = database.users.logIn("email.$number@opensavvy.dev", "123456789").valueOrThrow

		database.users.verifyToken(user, token1).valueOrThrow
		database.users.verifyToken(user, token2).valueOrThrow

		log.info { "Setting a password too short is forbidden" }

		assertFails { database.users.setPassword(user, "12345789", "1234").valueOrThrow }

		log.info { "Editing the password" }

		database.users.setPassword(user, "123456789", "456789123").valueOrThrow

		// The previous tokens should not be valid anymore…
		assertFails { database.users.verifyToken(user, token1).valueOrThrow }
		assertFails { database.users.verifyToken(user, token2).valueOrThrow }

		// …but it should be possible to create new ones
		val (_, token3) = database.users.logIn("email.$number@opensavvy.dev", "456789123").valueOrThrow
		val (_, token4) = database.users.logIn("email.$number@opensavvy.dev", "456789123").valueOrThrow

		database.users.verifyToken(user, token3).valueOrThrow
		database.users.verifyToken(user, token4).valueOrThrow

		log.info { "The administrator can reset the account, invalidating all tokens…" }

		val newPassword = database.users.resetPassword(user).valueOrThrow
		assertTrue(user.requestValueOrThrow().forceResetPassword)

		// The original tokens should of course still be invalid
		assertFails { database.users.verifyToken(user, token1).valueOrThrow }
		assertFails { database.users.verifyToken(user, token2).valueOrThrow }

		// The previous ones should be invalid as well
		assertFails { database.users.verifyToken(user, token3).valueOrThrow }
		assertFails { database.users.verifyToken(user, token4).valueOrThrow }

		// The new password can be used to log in a single time
		val (_, token5) = database.users.logIn("email.$number@opensavvy.dev", newPassword).valueOrThrow
		assertFails { database.users.logIn("email.$number@opensavvy.dev", newPassword).valueOrThrow }

		database.users.verifyToken(user, token5).valueOrThrow

		log.info { "Closing…" }

		database.users.edit(user, open = false).valueOrThrow

		currentCoroutineContext().cancelChildren()
	}
}
