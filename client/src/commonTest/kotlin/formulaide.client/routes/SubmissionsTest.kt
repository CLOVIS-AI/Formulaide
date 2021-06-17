package formulaide.client.routes

import formulaide.api.data.Data
import formulaide.api.data.Data.Simple.SimpleDataId.TEXT
import formulaide.api.data.Form
import formulaide.api.data.FormField
import formulaide.api.data.FormSubmission
import formulaide.api.types.Arity
import formulaide.client.runTest
import formulaide.client.testAdministrator
import formulaide.client.testClient
import kotlin.test.Test

class SubmissionsTest {

	@Test
	fun simple() = runTest {
		val admin = testAdministrator()

		val form = admin.createForm(Form(
			"Formulaire bête",
			0,
			open = true,
			public = true,
			fields = listOf(FormField(
				0,
				order = 1,
				arity = Arity.mandatory(),
				name = "Test",
				data = Data.simple(TEXT)
			)),
			actions = emptyList()
		))

		val client = testClient()

		client.submitForm(FormSubmission(
			form.id,
			mapOf(
				"0" to "Ma donnée de test"
			)
		))
	}

}
