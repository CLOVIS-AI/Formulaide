package formulaide.db

import formulaide.api.data.Data
import formulaide.api.data.Data.Simple.SimpleDataId.TEXT
import formulaide.api.data.Form
import formulaide.api.data.FormField
import formulaide.api.data.FormSubmission.Companion.createSubmission
import formulaide.api.types.Arity
import formulaide.db.document.createForm
import formulaide.db.document.saveSubmission
import kotlinx.coroutines.runBlocking
import kotlin.test.Test

class SubmissionsTest {

	@Test
	fun simple() = runBlocking {
		val db = testDatabase()

		val lastName = FormField(
			id = 1,
			order = 1,
			arity = Arity.mandatory(),
			name = "Nom",
			data = Data.simple(TEXT)
		)

		val firstName = FormField(
			id = 2,
			order = 2,
			arity = Arity.optional(),
			name = "Prénom",
			data = Data.simple(TEXT)
		)

		val form = db.createForm(
			Form(
				name = "Un formulaire intéressant",
				id = 0,
				open = true,
				public = true,
				fields = listOf(
					lastName,
					firstName
				),
				actions = emptyList()
			)
		)

		val submission1 = form.createSubmission(emptyList()) {
			text(lastName, "Mon nom de famille")
			text(firstName, "Mon prénom")
		}

		val submission2 = form.createSubmission(emptyList()) {
			text(lastName, "Mon nom de famille 2")
			text(firstName, "Mon prénom 2")
		}

		db.saveSubmission(submission1)
		db.saveSubmission(submission2)
		Unit
	}
}
