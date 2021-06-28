package formulaide.db

import formulaide.api.data.Form
import formulaide.api.data.FormSubmission.Companion.createSubmission
import formulaide.api.fields.FormField
import formulaide.api.fields.FormRoot
import formulaide.api.fields.SimpleField.Text
import formulaide.api.types.Arity
import formulaide.db.document.createForm
import formulaide.db.document.saveSubmission
import kotlinx.coroutines.runBlocking
import kotlin.test.Test

class SubmissionsTest {

	@Test
	fun simple() = runBlocking {
		val db = testDatabase()

		val lastName = FormField.Shallow.Simple(
			id = "1",
			order = 1,
			name = "Nom",
			Text(Arity.mandatory())
		)

		val firstName = FormField.Shallow.Simple(
			id = "2",
			order = 2,
			name = "Prénom",
			Text(Arity.optional())
		)

		val form = db.createForm(
			Form(
				name = "Un formulaire intéressant",
				id = "0",
				open = true,
				public = true,
				mainFields = FormRoot(setOf(
					lastName,
					firstName
				)),
				actions = emptyList()
			)
		)

		val submission1 = form.createSubmission {
			text(lastName, "Mon nom de famille")
			text(firstName, "Mon prénom")
		}

		val submission2 = form.createSubmission {
			text(lastName, "Mon nom de famille 2")
			text(firstName, "Mon prénom 2")
		}

		db.saveSubmission(submission1)
		db.saveSubmission(submission2)
		Unit
	}
}
