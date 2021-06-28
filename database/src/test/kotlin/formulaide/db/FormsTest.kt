package formulaide.db

import formulaide.api.data.Action
import formulaide.api.data.Form
import formulaide.api.fields.FormField
import formulaide.api.fields.FormRoot
import formulaide.api.fields.SimpleField.Text
import formulaide.api.types.Arity
import formulaide.db.document.createForm
import formulaide.db.document.listForms
import kotlinx.coroutines.runBlocking
import kotlin.test.Test

class FormsTest {

	@Test
	fun listForms() = runBlocking {
		val db = testDatabase()

		val forms = db.listForms(public = true)

		println(forms)
	}

	@Test
	fun create() = runBlocking {
		val db = testDatabase()

		db.createForm(Form(
			name = "Le formulaire des tests",
			id = "0",
			open = true,
			public = true,
			mainFields = FormRoot(setOf(
				FormField.Shallow.Simple(
					id = "1",
					order = 1,
					name = "Numéro fiscal",
					Text(Arity.mandatory())
				)
			)),
			actions = listOf(
				Action.ServiceReview(
					id = 1,
					order = 1,
					service = 1,
				)
			)
		))
		Unit
	}

}
