package formulaide.client.routes

import formulaide.api.data.*
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
					minArity = 1u,
					maxArity = 1u,
					name = "Nom de famille",
					type = Data.simple(DataId.TEXT)
				),
				CompoundDataField(
					order = 2,
					id = 2,
					minArity = 1u,
					maxArity = 1u,
					name = "Prénom",
					type = Data.simple(DataId.TEXT)
				),
				CompoundDataField(
					order = 3,
					id = 3,
					minArity = 0u,
					maxArity = 1u,
					name = "Numéro de téléphone",
					type = Data.simple(DataId.INTEGER)
				),
				CompoundDataField(
					order = 4,
					id = 4,
					minArity = 0u,
					maxArity = 30u,
					name = "Enfants à charge",
					type = Data.compound(SPECIAL_TOKEN_RECURSION)
				)
			)
		))
	}

}
