package formulaide.client.routes

import formulaide.client.runTest
import formulaide.client.testAdministrator
import formulaide.client.testEmployee
import kotlin.test.Test
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ServicesTest {

	@Test
	fun list() = runTest {
		val client = testEmployee()

		val services = client.listServices()

		assertTrue(services.isNotEmpty())
		assertTrue(services.all { it.open })
	}

	@Test
	fun fullList() = runTest {
		val client = testAdministrator()

		val services = client.listAllServices()

		assertTrue(services.isNotEmpty())
	}

	@Test
	fun createAsEmployee() = runTest {
		val client = testEmployee()

		assertFails {
			client.createService("Un super service qui ne peut pas être créé")
		}
	}

	@Test
	fun createAsAdmin() = runTest {
		val client = testAdministrator()

		val service = client.createService("Un super service qui peut être créé")

		assertTrue(service.open)
	}

	@Test
	fun close() = runTest {
		val client = testAdministrator()

		var service = client.createService("Un service inutile")
		assertTrue(service.open)

		service = client.closeService(service)
		assertFalse(service.open)

		service = client.reopenService(service)
		assertTrue(service.open)
	}

}
