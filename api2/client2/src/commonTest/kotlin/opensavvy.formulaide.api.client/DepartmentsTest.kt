@file:OptIn(ExperimentalCoroutinesApi::class)

package opensavvy.formulaide.api.client

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.test.runTest
import opensavvy.backbone.Ref.Companion.requestValue
import opensavvy.logger.Logger.Companion.info
import opensavvy.logger.loggerFor
import opensavvy.state.firstResultOrThrow
import kotlin.test.*

class DepartmentsTest {

	private val log = loggerFor(this)

	@Test
	fun create() = runTest {
		val client = testAdministrator()

		log.info { "Getting the original list…" }
		val before = client.departments.list().firstResultOrThrow()

		log.info { "Creating the department…" }
		val ref = client.departments.create("Test department creation").firstResultOrThrow()
		assertFalse(ref in before, "The department we just created already existed before: $ref in $before")

		log.info { "Checking that the database is not empty…" }
		val after = client.departments.list().firstResultOrThrow()
		assertTrue(ref in after, "The department we just created is not in the list of departments: $ref in $after")
		assertEquals("Test department creation", ref.requestValue().name)
		assertEquals(true, ref.requestValue().open)

		log.info { "Closing the department" }
		ref.close().firstResultOrThrow()

		currentCoroutineContext().cancelChildren()
	}

	@Test
	fun open() = runTest {
		val client = testAdministrator()

		log.info { "Creating a new department…" }
		val ref = client.departments.create("Test department opening").firstResultOrThrow()
		assertTrue(ref.requestValue().open)

		log.info { "Closing…" }
		ref.close().firstResultOrThrow()
		assertFalse(ref.requestValue().open)
		assertContains(client.departments.list(includeClosed = true).firstResultOrThrow(), ref)

		log.info { "Reopening…" }
		ref.open().firstResultOrThrow()
		assertTrue(ref.requestValue().open)

		log.info { "Re-closing…" }
		ref.close().firstResultOrThrow()
		assertFalse(ref.requestValue().open)

		currentCoroutineContext().cancelChildren()
	}

}
