package formulaide.db

import formulaide.db.document.DbUser
import formulaide.db.document.createUser
import formulaide.db.document.findUser
import kotlinx.coroutines.runBlocking
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class UsersTest {

	@Test
	fun createUser() = runBlocking {
		val db = testDatabase()

		val expected = DbUser("123456", "random@gmail.fr", "123456789", "My Other Name", db.testService().id, false)

		val actual = db.createUser(expected)
		assertEquals(expected, actual)
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
