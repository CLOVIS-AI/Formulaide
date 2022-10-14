package formulaide.db

import formulaide.db.document.DbUser
import formulaide.db.document.toCore
import kotlinx.coroutines.runBlocking
import opensavvy.backbone.Ref.Companion.requestValue
import opensavvy.state.firstResultOrThrow
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue

class UsersTest {

	@Test
	fun createUser() = runBlocking {
		val db = testDatabase()
		val email = "random${Random.nextInt()}@gmail.fr"
		val service = db.testService().firstResultOrThrow()

		val expected = DbUser(
			Random.nextInt().toString(),
			email,
			"123456789",
			"My Other Name",
			services = setOf(service.id.toInt()),
			isAdministrator = false
		)

		val actual = db.users.create(
			email,
			"My Other Name",
			setOf(service),
			administrator = false,
			password = "123456789"
		).requestValue()
		assertEquals(expected.toCore(db), actual)
	}

	@Test
	fun createDuplicateUser() = runBlocking {
		val db = testDatabase()
		val email = "random${Random.nextInt()}@gmail.fr"
		val service = db.testService().firstResultOrThrow()

		db.users.create(
			email,
			"My Other Name",
			setOf(service),
			administrator = true,
			password = "123456789"
		).requestValue()

		val failure = assertFails {
			db.users.create(
				email,
				"My Other Name",
				setOf(service),
				administrator = true,
				password = "123456789"
			).requestValue()
		}
		assertTrue(failure is IllegalStateException)
	}

	@Test
	fun findUser() = runBlocking {
		val db = testDatabase()
		val email = "random+${Random.nextInt()}@email.fr"
		val service = db.testService().firstResultOrThrow()

		db.users.create(
			email,
			"My Other Name",
			setOf(service),
			administrator = true,
			password = "123456789"
		).requestValue()

		val found = db.users.fromId(email).requestValue()

		assertEquals(email, found.email)
	}

}
