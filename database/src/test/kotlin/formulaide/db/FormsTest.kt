package formulaide.db

import formulaide.api.data.Action
import formulaide.api.dsl.form
import formulaide.api.dsl.formRoot
import formulaide.api.dsl.simple
import formulaide.api.fields.SimpleField.Text
import formulaide.api.types.Arity
import formulaide.api.types.Ref
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

		db.createForm(form(
			"Le formulaire des tests",
			public = true,
			formRoot {
				simple("Numéro fiscal", Text(Arity.mandatory()))
			},
			Action(
				id = "1",
				order = 1,
				Ref("1"),
				name = "Étape 1"
			)
		))
		Unit
	}

}
