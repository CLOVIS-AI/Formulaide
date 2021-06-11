package formulaide.db

import formulaide.api.data.CompoundDataField
import formulaide.api.data.Data
import formulaide.api.data.Data.Simple.SimpleDataId.TEXT
import formulaide.api.data.NewCompoundData
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
				minArity = 1,
				maxArity = 1,
				name = "Test",
				type = Data.simple(TEXT)
			)
		)))

		assertEquals(name, data.name)

		data.fields[0].run {
			assertEquals(1, id)
			assertEquals(1, order)
			assertEquals(1, minArity)
			assertEquals(1, maxArity)
			assertEquals("Test", this.name)
			assertEquals(Data.simple(TEXT), type)
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
				minArity = 1,
				maxArity = 1,
				name = "Nom complet",
				type = Data.simple(TEXT)
			),
			CompoundDataField(
				id = 2,
				order = 2,
				minArity = 1,
				maxArity = 1,
				name = "Famille",
				type = Data.recursiveCompound()
			)
		)))

		assertEquals(name, data.name)

		val fullName = CompoundDataField(
			id = 1,
			order = 1,
			minArity = 1,
			maxArity = 1,
			name = "Nom complet",
			type = Data.simple(TEXT)
		)

		val family = CompoundDataField(
			id = 2,
			order = 2,
			minArity = 1,
			maxArity = 1,
			name = "Famille",
			type = Data.compound(data)
		)

		assertEquals(fullName, data.fields.find { it.id == fullName.id })
		assertEquals(family, data.fields.find { it.id == family.id })
	}

}
