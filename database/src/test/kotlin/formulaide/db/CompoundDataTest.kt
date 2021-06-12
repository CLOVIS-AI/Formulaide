package formulaide.db

import formulaide.api.data.CompoundDataField
import formulaide.api.data.Data
import formulaide.api.data.Data.Simple.SimpleDataId.TEXT
import formulaide.api.data.NewCompoundData
import formulaide.api.types.Arity
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
				arity = Arity.mandatory(),
				name = "Test",
				data = Data.simple(TEXT)
			)
		)))

		assertEquals(name, data.name)

		data.fields[0].run {
			assertEquals(1, id)
			assertEquals(1, order)
			assertEquals(1, arity.min)
			assertEquals(1, arity.max)
			assertEquals("Test", this.name)
			assertEquals(Data.simple(TEXT), this.data)
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
				arity = Arity.mandatory(),
				name = "Nom complet",
				data = Data.simple(TEXT)
			),
			CompoundDataField(
				id = 2,
				order = 2,
				arity = Arity.mandatory(),
				name = "Famille",
				data = Data.recursiveCompound()
			)
		)))

		assertEquals(name, data.name)

		val fullName = CompoundDataField(
			id = 1,
			order = 1,
			arity = Arity.mandatory(),
			name = "Nom complet",
			data = Data.simple(TEXT)
		)

		val family = CompoundDataField(
			id = 2,
			order = 2,
			arity = Arity.mandatory(),
			name = "Famille",
			data = Data.compound(data)
		)

		assertEquals(fullName, data.fields.find { it.id == fullName.id })
		assertEquals(family, data.fields.find { it.id == family.id })
	}

}
