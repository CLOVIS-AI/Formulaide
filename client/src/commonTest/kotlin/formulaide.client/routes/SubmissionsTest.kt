@file:OptIn(ExperimentalCoroutinesApi::class)

package formulaide.client.routes

import formulaide.api.data.Action
import formulaide.api.data.FormSubmission.Companion.createSubmission
import formulaide.api.dsl.form
import formulaide.api.dsl.formRoot
import formulaide.api.dsl.simple
import formulaide.api.fields.FormField
import formulaide.api.fields.SimpleField.Text
import formulaide.api.types.Arity
import formulaide.api.types.Ref
import formulaide.client.testAdministrator
import formulaide.client.testClient
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class SubmissionsTest {

	@Test
	fun simple() = runTest {
		val admin = testAdministrator()

		lateinit var test: FormField.Simple

		val form = admin.createForm(form(
			"Formulaide bête",
			public = true,
			mainFields = formRoot {
				test = simple("Test", Text(Arity.mandatory()))
			},
			Action("0", order = 0, reviewer = Ref("0"), name = "Validés")
		))

		val client = testClient()

		client.submitForm(form.createSubmission {
			text(test, "Ma donnée de test")
		})
	}

}
