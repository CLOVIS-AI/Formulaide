package formulaide.db

import formulaide.api.data.Action
import formulaide.api.dsl.form
import formulaide.api.dsl.formRoot
import formulaide.api.dsl.simple
import formulaide.api.fields.SimpleField.Text
import formulaide.api.types.Arity
import formulaide.api.types.Ref
import formulaide.db.document.createLegacyForm
import formulaide.db.document.listLegacyForms
import kotlinx.coroutines.runBlocking
import kotlin.test.Test

class FormsTest {

	@Test
	fun listForms() = runBlocking {
		val db = testDatabase()

		val forms = db.listLegacyForms(public = true)

		println(forms)
	}

	@Test
	fun create() = runBlocking {
		val db = testDatabase()

		db.createLegacyForm(
			form(
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
			)
		)
		Unit
	}

}
