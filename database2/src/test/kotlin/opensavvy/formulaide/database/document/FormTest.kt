@file:OptIn(ExperimentalCoroutinesApi::class)

package opensavvy.formulaide.database.document

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import opensavvy.backbone.Ref.Companion.requestValue
import opensavvy.formulaide.core.Field
import opensavvy.formulaide.core.Form
import opensavvy.formulaide.core.InputConstraints
import opensavvy.formulaide.core.Template
import opensavvy.formulaide.database.testDatabase
import opensavvy.logger.Logger.Companion.info
import opensavvy.logger.loggerFor
import opensavvy.state.firstResultOrThrow
import kotlin.test.Test

class FormTest {

	private val log = loggerFor(this)

	@Test
	fun create() = runTest {
		val database = testDatabase()

		log.info { "Creating a department…" }

		val dept = database.departments
			.create("Department for the form test")
			.firstResultOrThrow()

		log.info { "Creating the identity template…" }

		val identityFields = Field.Group(
			"Identity test",
			mapOf(
				0 to Field.Input("First name", InputConstraints.Text(maxLength = 20u), importedFrom = null),
				1 to Field.Input("Last name", InputConstraints.Text(maxLength = 20u), importedFrom = null),
			),
			importedFrom = null
		)

		val identity = database.templates
			.create("Identities for forms", Template.Version(Clock.System.now(), "First version", identityFields))
			.firstResultOrThrow()

		log.info { "Creating a new form…" }

		val initialFields = Field.Group(
			"Request",
			mapOf(
				0 to Field.Group(
					"Identity",
					mapOf(
						0 to Field.Input("First name", InputConstraints.Text(maxLength = 20u), importedFrom = null),
						1 to Field.Input("Last name", InputConstraints.Text(maxLength = 20u), importedFrom = null),
					),
					importedFrom = identity.requestValue().versions.first()
				),
				1 to Field.Input("Idea", InputConstraints.Text(maxLength = 256u), importedFrom = null),
			),
			importedFrom = null,
		)

		val request = database.forms
			.create(
				"Request test", public = true, Form.Version(
					Clock.System.now(),
					"First version",
					initialFields,
					listOf(
						Form.Step(0, dept, field = null),
						Form.Step(1, dept, field = null),
					)
				)
			)
			.firstResultOrThrow()

		log.info { "Closing…" }

		database.departments.close(dept).firstResultOrThrow()
		database.templates.edit(identity, open = false).firstResultOrThrow()
		database.forms.edit(request, open = false).firstResultOrThrow()

		currentCoroutineContext().cancelChildren()
	}

}
