package formulaide.client.routes

import formulaide.api.data.Action
import formulaide.api.dsl.form
import formulaide.api.dsl.formRoot
import formulaide.api.dsl.simple
import formulaide.api.fields.SimpleField.Text
import formulaide.api.types.Arity
import formulaide.client.runTest
import formulaide.client.testAdministrator
import formulaide.client.testClient
import formulaide.client.testEmployee
import kotlin.test.Test
import kotlin.test.assertTrue

class FormsTest {

	@Test
	fun create() = runTest {
		val admin = testAdministrator()
		val me = admin.getMe()

		val form = form(
			"Un autre formulaide de tests",
			public = true,
			mainFields = formRoot {
				simple("Nom de famille", Text(Arity.mandatory()))
				simple("Prénom", Text(Arity.optional()))
			},
			Action.ServiceReview(
				id = 1,
				order = 1,
				service = me.service
			),
			Action.EmployeeReview(
				id = 2,
				order = 2,
				employee = me
			)
		)

		admin.createForm(form)
	}

	@Test
	fun list() = runTest {
		val user = testClient()

		val forms = user.listForms()

		assertTrue(forms.all { it.open })
		assertTrue(forms.all { it.public })
	}

	@Test
	fun listAll() = runTest {
		val user = testEmployee()

		val forms = user.listAllForms()

		assertTrue(forms.all { it.open })
	}

	@Test
	fun listClosed() = runTest {
		val user = testAdministrator()

		user.listClosedForms()
	}

}
