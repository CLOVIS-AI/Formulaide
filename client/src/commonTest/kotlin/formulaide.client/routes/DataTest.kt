package formulaide.client.routes

import formulaide.api.data.CompoundDataField
import formulaide.api.data.Data
import formulaide.api.data.DataId
import formulaide.api.data.NewCompoundData
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

		user.createData(NewCompoundData(
			name = "Identité",
			fields = listOf(
				CompoundDataField(
					order = 1,
					id = 1,
					minArity = 1,
					maxArity = 1,
					name = "Nom de famille",
					type = Data.simple(DataId.TEXT)
				),
				CompoundDataField(
					order = 2,
					id = 2,
					minArity = 1,
					maxArity = 1,
					name = "Prénom",
					type = Data.simple(DataId.TEXT)
				),
				CompoundDataField(
					order = 3,
					id = 3,
					minArity = 0,
					maxArity = 1,
					name = "Numéro de téléphone",
					type = Data.simple(DataId.INTEGER)
				),
				CompoundDataField(
					order = 4,
					id = 4,
					minArity = 0,
					maxArity = 30,
					name = "Enfants à charge",
					type = Data.recursiveCompound()
				)
			)
		))
	}

}
