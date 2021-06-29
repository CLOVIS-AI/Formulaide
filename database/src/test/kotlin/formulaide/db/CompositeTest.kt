package formulaide.db

import formulaide.api.dsl.composite
import formulaide.api.dsl.compositeItself
import formulaide.api.dsl.simple
import formulaide.api.fields.DataField
import formulaide.api.fields.SimpleField.Text
import formulaide.api.types.Arity
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

		val data = db.createComposite(composite(name) {
			simple("Test", Text(Arity.mandatory()))
		})

		assertEquals(name, data.name)

		data.fields.first().run {
			assertEquals("0", id)
			assertEquals(0, order)
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

		val data = db.createComposite(composite(name) {
			simple("Nom complet", Text(Arity.mandatory()))
			compositeItself("Famille", Arity.optional())
		})

		assertEquals(name, data.name)

		val fullName = DataField.Simple(
			id = "0",
			order = 0,
			name = "Nom complet",
			Text(Arity.mandatory())
		)

		val family = DataField.Composite(
			id = "1",
			order = 1,
			arity = Arity.optional(),
			name = "Famille",
			ref = data.createRef()
		)

		assertEquals(fullName, data.fields.find { it.id == fullName.id })
		assertEquals(family, data.fields.find { it.id == family.id })
	}

}
