package formulaide.db

import formulaide.db.document.allServices
import formulaide.db.document.allServicesIgnoreOpen
import formulaide.db.document.createService
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ServicesTest {

	@Test
	fun list() = runBlocking {
		val db = testDatabase()

		val services = db.allServices()

		assertTrue(services.all { it.open }, "All services returned by this endpoint should be open")
	}

	@Test
	fun fullList() = runBlocking {
		val db = testDatabase()

		db.allServicesIgnoreOpen()
		Unit
	}

	@Test
	fun create() = runBlocking {
		val db = testDatabase()

		val name = "Service des tests créés automatiquement"

		val service = db.createService(name)

		assertEquals(name, service.name)
		assertTrue(service.open)
	}

}
