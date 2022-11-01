@file:OptIn(ExperimentalCoroutinesApi::class)

package opensavvy.formulaide.database.document

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import opensavvy.backbone.Ref.Companion.requestValueOrThrow
import opensavvy.formulaide.core.Field.Group
import opensavvy.formulaide.core.Field.Input
import opensavvy.formulaide.core.InputConstraints
import opensavvy.formulaide.core.Template
import opensavvy.formulaide.database.testDatabase
import opensavvy.logger.Logger.Companion.info
import opensavvy.logger.loggerFor
import opensavvy.state.slice.valueOrThrow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class TemplateTest {

	private val log = loggerFor(this)

	@Test
	fun create() = runTest {
		val database = testDatabase()

		log.info { "Creating the city template…" }

		val cityFields = Group(
			"City test",
			mapOf(
				0 to Input("Postal code", InputConstraints.Text(maxLength = 5u), importedFrom = null),
				1 to Input("Name", InputConstraints.Text(maxLength = 50u), importedFrom = null)
			),
			importedFrom = null,
		)

		val city = database.templates
			.create("Cities", Template.Version(Clock.System.now(), "First version", cityFields))
			.valueOrThrow

		run {
			val data = city.requestValueOrThrow()
			assertEquals("Cities", data.name)
			assertEquals(1, data.versions.size)
			assertEquals(cityFields, data.versions.first().requestValueOrThrow().field)
		}

		log.info { "Creating the identity template…" }

		val identityFields = Group(
			"Identity test",
			mapOf(
				0 to Input("First name", InputConstraints.Text(maxLength = 20u), importedFrom = null),
				1 to Input("Last name", InputConstraints.Text(maxLength = 20u), importedFrom = null),
				2 to Group(
					"Home town",
					mapOf(
						0 to Input("Postal code", InputConstraints.Text(maxLength = 5u), importedFrom = null),
						1 to Input("Name", InputConstraints.Text(maxLength = 50u), importedFrom = null)
					),
					importedFrom = city.requestValueOrThrow().versions.first()
				)
			),
			importedFrom = null
		)

		val identity = database.templates
			.create("Identities", Template.Version(Clock.System.now(), "First version", identityFields))
			.valueOrThrow

		run {
			val data = identity.requestValueOrThrow()
			assertEquals("Identities", data.name)
			assertEquals(1, data.versions.size)
			assertEquals(identityFields, data.versions.first().requestValueOrThrow().field)
		}

		log.info { "Creating a new version…" }

		// the difference is the max length of the text strings
		val identityFields2 = Group(
			"Identity test",
			mapOf(
				0 to Input("First name", InputConstraints.Text(maxLength = 50u), importedFrom = null),
				1 to Input("Last name", InputConstraints.Text(maxLength = 50u), importedFrom = null),
				2 to Group(
					"Home town",
					mapOf(
						0 to Input("Postal code", InputConstraints.Text(maxLength = 5u), importedFrom = null),
						1 to Input("Name", InputConstraints.Text(maxLength = 50u), importedFrom = null)
					),
					importedFrom = city.requestValueOrThrow().versions.first()
				)
			),
			importedFrom = null
		)

		assertNotEquals(identityFields2, identityFields)

		database.templates
			.createVersion(identity, Template.Version(Clock.System.now(), "Second version", identityFields2))
			.valueOrThrow

		run {
			val data = identity.requestValueOrThrow()
			assertEquals("Identities", data.name)
			assertEquals(2, data.versions.size)

			val (first, second) = data.versions.sortedBy { it.version }
			assertEquals(identityFields, first.requestValueOrThrow().field)
			assertEquals(identityFields2, second.requestValueOrThrow().field)
		}

		log.info { "Closing…" }

		database.templates.edit(city, open = false).valueOrThrow
		database.templates.edit(identity, open = false).valueOrThrow

		currentCoroutineContext().cancelChildren()
	}

}
