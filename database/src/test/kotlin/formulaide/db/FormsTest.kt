package formulaide.db

import formulaide.api.data.Action
import formulaide.api.data.Data
import formulaide.api.data.Data.Simple.SimpleDataId.TEXT
import formulaide.api.data.Form
import formulaide.api.data.FormField
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
			id = 0,
			open = true,
			public = true,
			fields = listOf(
				FormField(
					id = 1,
					order = 1,
					arity = Arity.mandatory(),
					name = "Numéro fiscal",
					data = Data.simple(TEXT)
				)
			),
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