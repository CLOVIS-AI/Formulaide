@file:OptIn(ExperimentalCoroutinesApi::class)

package opensavvy.formulaide.database.document

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.test.runTest
import opensavvy.backbone.Ref.Companion.requestValueOrThrow
import opensavvy.formulaide.database.testDatabase
import opensavvy.logger.Logger.Companion.info
import opensavvy.logger.loggerFor
import opensavvy.state.slice.valueOrThrow
import kotlin.test.*

class DepartmentTest {

	private val log = loggerFor(this)

	@Test
	fun create() = runTest {
		val database = testDatabase()

		log.info { "Getting the original list…" }
		val before = database.departments.list().valueOrThrow

		log.info { "Creating the department…" }
		val ref = database.departments.create("Test department creation").valueOrThrow
		assertFalse(ref in before, "The department we just created already existed before: $ref in $before")

		log.info { "Checking that the database is not empty…" }
		val after = database.departments.list().valueOrThrow
		assertTrue(ref in after, "The department we just created is not in the list of departments: $ref in $after")
		assertEquals("Test department creation", ref.requestValueOrThrow().name)
		assertEquals(true, ref.requestValueOrThrow().open)

		log.info { "Closing the department" }
		ref.close().valueOrThrow

		currentCoroutineContext().cancelChildren()
	}

	@Test
	fun open() = runTest {
		val database = testDatabase()

		log.info { "Creating a new department…" }
		val ref = database.departments.create("Test department opening").valueOrThrow
		assertTrue(ref.requestValueOrThrow().open)

		log.info { "Closing…" }
		ref.close().valueOrThrow
		assertFalse(ref.requestValueOrThrow().open)
		assertContains(database.departments.list(includeClosed = true).valueOrThrow, ref)

		log.info { "Reopening…" }
		ref.open().valueOrThrow
		assertTrue(ref.requestValueOrThrow().open)

		log.info { "Re-closing…" }
		ref.close().valueOrThrow
		assertFalse(ref.requestValueOrThrow().open)

		currentCoroutineContext().cancelChildren()
	}

}
