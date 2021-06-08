package formulaide.db

import formulaide.db.document.DbUser
import formulaide.db.document.createUser
import formulaide.db.document.findUser
import kotlinx.coroutines.runBlocking
import kotlin.random.Random
import kotlin.test.*

class UsersTest {

	@Test
	fun createUser() = runBlocking {
		val db = testDatabase()

		val expected = DbUser(
			"123456",
			"random${Random.nextInt()}@gmail.fr",
			"123456789",
			"My Other Name",
			db.testService().id,
			false
		)

		val actual = db.createUser(expected)
		assertEquals(expected, actual)
	}

	@Test
	fun createDuplicateUser() = runBlocking {
		val db = testDatabase()

		val user = DbUser(
			"123456",
			"r${Random.nextInt()}@gmail.fr",
			"123456789",
			"Name",
			db.testService().id,
			false
		)
		db.createUser(user)

		val failure = assertFails {
			db.createUser(user)
		}
		assertTrue(failure is IllegalStateException)
	}

	@Test
	fun findUser() = runBlocking {
		val db = testDatabase()

		val email = "random+${Random.nextInt()}@email.fr"

		val user = DbUser("…", email, "…", "Some Other Name", db.testService().id, false)
		db.createUser(user)

		val found = db.findUser(email)

		assertNotNull(found)
		assertEquals(email, found.email)
	}

}
