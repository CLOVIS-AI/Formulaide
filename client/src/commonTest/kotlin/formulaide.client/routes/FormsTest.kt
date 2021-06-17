package formulaide.client.routes

import formulaide.api.data.Action
import formulaide.api.data.Data
import formulaide.api.data.Data.Simple.SimpleDataId.TEXT
import formulaide.api.data.Form
import formulaide.api.data.FormField
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

		val form = Form(
			name = "Un autre formulaire de tests",
			id = 0,
			open = true,
			public = true,
			fields = listOf(
				FormField(
					id = 1,
					order = 1,
					name = "Nom de famille",
					arity = Arity.mandatory(),
					data = Data.simple(TEXT)
				),
				FormField(
					id = 2,
					order = 2,
					name = "Prénom",
					arity = Arity.optional(),
					data = Data.simple(TEXT)
				)
			),
			actions = listOf(
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
