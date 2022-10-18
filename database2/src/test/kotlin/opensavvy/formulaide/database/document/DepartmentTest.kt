@file:OptIn(ExperimentalCoroutinesApi::class)

package opensavvy.formulaide.database.document

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.test.runTest
import opensavvy.backbone.Ref.Companion.requestValue
import opensavvy.formulaide.database.testDatabase
import opensavvy.logger.Logger.Companion.info
import opensavvy.logger.loggerFor
import opensavvy.state.firstResultOrThrow
import kotlin.test.*

class DepartmentTest {

	private val log = loggerFor(this)

	@Test
	fun create() = runTest {
		val database = testDatabase()

		log.info { "Getting the original list…" }
		val before = database.departments.list().firstResultOrThrow()

		log.info { "Creating the department…" }
		val ref = database.departments.create("Test department creation").firstResultOrThrow()
		assertFalse(ref in before, "The department we just created already existed before: $ref in $before")

		log.info { "Checking that the database is not empty…" }
		val after = database.departments.list().firstResultOrThrow()
		assertTrue(ref in after, "The department we just created is not in the list of departments: $ref in $after")
		assertEquals("Test department creation", ref.requestValue().name)
		assertEquals(true, ref.requestValue().open)

		log.info { "Closing the department" }
		ref.close().firstResultOrThrow()

		currentCoroutineContext().cancelChildren()
	}

	@Test
	fun open() = runTest {
		val database = testDatabase()

		log.info { "Creating a new department…" }
		val ref = database.departments.create("Test department opening").firstResultOrThrow()
		assertTrue(ref.requestValue().open)

		log.info { "Closing…" }
		ref.close().firstResultOrThrow()
		assertFalse(ref.requestValue().open)
		assertContains(database.departments.list(includeClosed = true).firstResultOrThrow(), ref)

		log.info { "Reopening…" }
		ref.open().firstResultOrThrow()
		assertTrue(ref.requestValue().open)

		log.info { "Re-closing…" }
		ref.close().firstResultOrThrow()
		assertFalse(ref.requestValue().open)

		currentCoroutineContext().cancelChildren()
	}

}
