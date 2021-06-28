package formulaide.db

import formulaide.api.data.Composite
import formulaide.api.data.SPECIAL_TOKEN_RECURSION
import formulaide.api.fields.DataField
import formulaide.api.fields.SimpleField.Text
import formulaide.api.types.Arity
import formulaide.api.types.Ref
import formulaide.api.types.Ref.Companion.createRef
import formulaide.db.document.createComposite
import formulaide.db.document.listComposites
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class CompositeTest {

	@Test
	fun listData() = runBlocking {
		val db = testDatabase()

		db.listComposites()
		Unit
	}

	@Test
	fun createSimpleData() = runBlocking {
		val db = testDatabase()

		val name = "Identité plate"

		val data = db.createComposite(Composite("", name, listOf(
			DataField.Simple(
				id = "1",
				order = 1,
				name = "Test",
				Text(Arity.mandatory())
			)
		)))

		assertEquals(name, data.name)

		data.fields.first().run {
			assertEquals("1", id)
			assertEquals(1, order)
			assertEquals(1, arity.min)
			assertEquals(1, arity.max)
			assertEquals("Test", this.name)
			assertEquals(Text(Arity.mandatory()), (this as DataField.Simple).simple)
		}
	}

	@Test
	fun createRecursiveData() = runBlocking {
		val db = testDatabase()

		val name = "Identité récursive"

		val data = db.createComposite(Composite("", name, listOf(
			DataField.Simple(
				id = "1",
				order = 1,
				name = "Nom complet",
				Text(Arity.mandatory())
			),
			DataField.Composite(
				id = "2",
				order = 2,
				arity = Arity.mandatory(),
				name = "Famille",
				ref = Ref(SPECIAL_TOKEN_RECURSION)
			)
		)))

		assertEquals(name, data.name)

		val fullName = DataField.Simple(
			id = "1",
			order = 1,
			name = "Nom complet",
			Text(Arity.mandatory())
		)

		val family = DataField.Composite(
			id = "2",
			order = 2,
			arity = Arity.mandatory(),
			name = "Famille",
			ref = data.createRef()
		)

		assertEquals(fullName, data.fields.find { it.id == fullName.id })
		assertEquals(family, data.fields.find { it.id == family.id })
	}

}
