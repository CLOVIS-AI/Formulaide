package formulaide.client.routes

import formulaide.api.data.Action
import formulaide.api.dsl.*
import formulaide.api.fields.DataField
import formulaide.api.fields.SimpleField.Message
import formulaide.api.fields.SimpleField.Text
import formulaide.api.types.Arity
import formulaide.api.types.Ref
import formulaide.client.*
import kotlinx.serialization.encodeToString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FormsTest {

	@Test
	fun create() = runTest {
		val admin = testAdministrator()
		val me = admin.getMe()

		val form = form(
			"Un autre formulaide de tests",
			public = true,
			mainFields = formRoot {
				simple("Nom de famille", Text(Arity.mandatory()))
				simple("Prénom", Text(Arity.optional()))
			},
			Action(
				id = "1",
				order = 1,
				me.services.first(),
				name = "Vérification de l'identité"
			),
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

	@Test
	fun references() = runTest {
		val admin = testAdministrator()

		lateinit var firstSimple: DataField.Simple
		lateinit var firstUnion: DataField.Union
		lateinit var firstUnionChoice1: DataField.Simple
		lateinit var firstUnionChoice2: DataField.Simple
		val composite1 = composite("Donnée référencée 1") {
			firstSimple = simple("Premier champ", Message)
			firstUnion = union("Deuxième champ", Arity.mandatory()) {
				firstUnionChoice1 = simple("Première option", Message)
				firstUnionChoice2 = simple("Deuxième option", Message)
			}
		}.let { admin.createData(it) }

		lateinit var secondSimple: DataField.Simple
		val composite2 = composite("Donnée référencée 2") {
			secondSimple = simple("Premier champ", Message)
		}.let { admin.createData(it) }

		lateinit var thirdSimple: DataField.Simple
		lateinit var thirdComposite: DataField.Composite
		val composite3 = composite("Donnée référencée 3", composite2) {
			thirdSimple = simple("Premier champ", Message)
			thirdComposite = composite("Référence 2", Arity.optional(), composite2)
		}.let { admin.createData(it) }

		val form = form(
			"Test de référence de données",
			public = true,
			formRoot(composite1, composite2, composite3) {
				simple("Premier champ", Message)

				composite("Référence 1", Arity.mandatory(), composite1) {
					simple(firstSimple, firstSimple.simple)
					union(firstUnion, Arity.mandatory()) {
						simple(firstUnionChoice1, firstUnionChoice1.simple)
						simple(firstUnionChoice2, firstUnionChoice2.simple)
					}
				}

				composite("Référence 3", Arity.mandatory(), composite3) {
					simple(thirdSimple, thirdSimple.simple)
					composite(thirdComposite, Arity.mandatory()) {
						simple(secondSimple, secondSimple.simple)
					}
				}
			},
			Action("0", order = 0, reviewer = Ref("0"), name = "Validés"),
		).let { admin.createForm(it) }

		println(form)
		println(Client.jsonSerializer.encodeToString(form))
		val references = admin.compositesReferencedIn(form)

		assertEquals(3, references.size, "Found $references")
		for (composite in listOf(composite1, composite2, composite3))
			assertTrue(references.any { it.id == composite.id })
	}

}
