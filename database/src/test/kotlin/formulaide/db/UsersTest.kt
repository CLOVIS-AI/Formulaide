package formulaide.db

import arrow.core.Either.Right
import formulaide.db.document.DbUser
import kotlinx.coroutines.runBlocking
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class UsersTest {

	@Test
	fun createUser() = runBlocking {
		val db = testDatabase()

		val expected = DbUser("123456", "random@gmail.fr", "123456789", "My Other Name")

		val actual = db.createUser(expected)

		assertTrue(actual is Right<DbUser>, "Found $actual")
		assertEquals(expected, actual.value)
	}

	@Test
	fun findUser() = runBlocking {
		val db = testDatabase()

		val email = "random+${Random.nextInt()}@email.fr"

		val user = DbUser("…", email, "…", "Some Other Name")
		val insert = db.createUser(user)
		assertTrue(insert is Right, "Found $insert")

		val found = db.findUser(email)
		assertTrue(found is Right, "Found $found")

		val foundUser = found.value
		assertNotNull(foundUser)
		assertEquals(email, foundUser.email)
	}

}
