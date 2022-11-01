package formulaide.db

import kotlinx.coroutines.runBlocking
import opensavvy.backbone.Ref.Companion.requestValueOrThrow
import opensavvy.state.firstValueOrThrow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ServicesTest {

	@Test
	fun list() = runBlocking {
		val db = testDatabase()

		val services = db.departments.all().firstValueOrThrow().map { it.requestValueOrThrow() }

		assertTrue(services.all { it.open }, "All services returned by this endpoint should be open")
	}

	@Test
	fun fullList() = runBlocking {
		val db = testDatabase()

		db.departments.all(includeClosed = true)
		Unit
	}

	@Test
	fun create() = runBlocking {
		val db = testDatabase()

		val name = "Service des tests créés automatiquement"

		val dep = db.departments.create(name)
			.firstValueOrThrow()
			.requestValueOrThrow()

		assertEquals(name, dep.name)
		assertTrue(dep.open)
	}

}
