@file:OptIn(ExperimentalCoroutinesApi::class)

package formulaide.client.routes

import formulaide.api.dsl.composite
import formulaide.api.dsl.compositeItself
import formulaide.api.dsl.simple
import formulaide.api.fields.SimpleField.Integer
import formulaide.api.fields.SimpleField.Text
import formulaide.api.types.Arity
import formulaide.api.types.Arity.Companion.mandatory
import formulaide.api.types.Arity.Companion.optional
import formulaide.client.testAdministrator
import formulaide.client.testEmployee
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class DataTest {

	@Test
	fun list() = runTest {
		val user = testEmployee()

		user.listData()
	}

	@Test
	fun create() = runTest {
		val user = testAdministrator()

		val newData = composite("Identité") {
			simple("Nom de famille", Text(mandatory()))
			simple("Prénom", Text(mandatory()))
			simple("Numéro de téléphone", Integer(optional()))
			compositeItself("Enfants à charge", Arity.list(0, 30))
		}
		user.createData(newData)
	}
}
