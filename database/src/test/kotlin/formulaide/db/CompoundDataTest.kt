package formulaide.db

import formulaide.api.data.*
import formulaide.db.document.createData
import formulaide.db.document.listData
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class CompoundDataTest {

	@Test
	fun listData() = runBlocking {
		val db = testDatabase()

		db.listData()
		Unit
	}

	@Test
	fun createSimpleData() = runBlocking {
		val db = testDatabase()

		val name = "Identité plate"

		val data = db.createData(NewCompoundData(name, listOf(
			CompoundDataField(
				id = 1,
				order = 1,
				minArity = 1u,
				maxArity = 1u,
				name = "Test",
				type = Data(DataId.TEXT)
			)
		)))

		assertEquals(name, data.name)

		data.fields[0].run {
			assertEquals(1, id)
			assertEquals(1, order)
			assertEquals(1u, minArity)
			assertEquals(1u, maxArity)
			assertEquals("Test", this.name)
			assertEquals(Data.simple(DataId.TEXT), type)
		}
	}

	@Test
	fun createRecursiveData() = runBlocking {
		val db = testDatabase()

		val name = "Identité récursive"

		val data = db.createData(NewCompoundData(name, listOf(
			CompoundDataField(
				id = 1,
				order = 1,
				minArity = 1u,
				maxArity = 1u,
				name = "Nom complet",
				type = Data(DataId.TEXT)
			),
			CompoundDataField(
				id = 2,
				order = 2,
				minArity = 1u,
				maxArity = 1u,
				name = "Famille",
				type = Data.compound(SPECIAL_TOKEN_RECURSION)
			)
		)))

		assertEquals(name, data.name)
		val id = data.id

		val fullName = CompoundDataField(
			id = 1,
			order = 1,
			minArity = 1u,
			maxArity = 1u,
			name = "Nom complet",
			type = Data(DataId.TEXT)
		)

		val family = CompoundDataField(
			id = 2,
			order = 2,
			minArity = 1u,
			maxArity = 1u,
			name = "Famille",
			type = Data.compound(id)
		)

		assertEquals(fullName, data.fields.find { it.id == fullName.id })
		assertEquals(family, data.fields.find { it.id == family.id })
	}

}
