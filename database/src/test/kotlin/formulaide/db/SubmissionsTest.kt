package formulaide.db

import formulaide.api.data.FormSubmission.Companion.createSubmission
import formulaide.api.dsl.form
import formulaide.api.dsl.formRoot
import formulaide.api.dsl.simple
import formulaide.api.fields.FormField
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

		lateinit var lastName: FormField.Simple
		lateinit var firstName: FormField.Simple

		val form = db.createForm(form(
			"Un formulaire intéressant",
			public = true,
			mainFields = formRoot {
				lastName = simple("Nom", Text(Arity.mandatory()))
				firstName = simple("Prénom", Text(Arity.optional()))
			}
		))

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
