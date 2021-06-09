package formulaide.db

import formulaide.db.document.allServices
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertTrue

class ServicesTest {

	@Test
	fun list() = runBlocking {
		val db = testDatabase()

		db.allServices()

		assertTrue(true) // exception is thrown previously on failure
	}

}
