package formulaide.client.routes

import formulaide.api.data.CompoundDataField
import formulaide.api.data.Data
import formulaide.api.data.Data.Simple.SimpleDataId.INTEGER
import formulaide.api.data.Data.Simple.SimpleDataId.TEXT
import formulaide.api.data.NewCompoundData
import formulaide.api.types.Arity
import formulaide.api.types.Arity.Companion.asArity
import formulaide.client.runTest
import formulaide.client.testAdministrator
import formulaide.client.testEmployee
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

		val newData = NewCompoundData(
			name = "Identité",
			fields = listOf(
				CompoundDataField(
					order = 1,
					id = 1,
					arity = Arity.mandatory(),
					name = "Nom de famille",
					data = Data.simple(TEXT)
				),
				CompoundDataField(
					order = 2,
					id = 2,
					arity = Arity.mandatory(),
					name = "Prénom",
					data = Data.simple(TEXT)
				),
				CompoundDataField(
					order = 3,
					id = 3,
					arity = Arity.optional(),
					name = "Numéro de téléphone",
					data = Data.simple(INTEGER)
				),
				CompoundDataField(
					order = 4,
					id = 4,
					arity = (0..30).asArity(),
					name = "Enfants à charge",
					data = Data.recursiveCompound()
				)
			)
		)
		user.createData(newData)
	}

}
