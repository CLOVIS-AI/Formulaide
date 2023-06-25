package opensavvy.formulaide.core

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import opensavvy.formulaide.core.Field.Companion.arity
import opensavvy.formulaide.core.Field.Companion.choice
import opensavvy.formulaide.core.Field.Companion.group
import opensavvy.formulaide.core.Field.Companion.input
import opensavvy.formulaide.core.Field.Companion.label
import kotlin.js.JsName
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class FieldTest {

	@Test
	@JsName("labelBuilder")
	fun `label builder`() = runTest {
		val field = label("test label")

		assertEquals("test label", field.label)
		assertEquals(emptyMap(), field.indexedFields)
		assertEquals(emptyList(), field.fields.toList())
		assertEquals(null, field.importedFrom)
	}

	@Test
	@JsName("inputBuilder")
	fun `input builder`() = runTest {
		val field = input("test label", Input.Toggle)

		assertEquals("test label", field.label)
		assertEquals(emptyMap(), field.indexedFields)
		assertEquals(emptyList(), field.fields.toList())
		assertEquals(null, field.importedFrom)

		assertEquals(Input.Toggle, field.input)
	}

	@Test
	@JsName("choiceBuilder")
	fun `choice builder`() = runTest {
		val field = choice(
			"test label",
			0 to label("field 0"),
			1 to input("field 1", Input.Email),
		)

		assertEquals("test label", field.label)
		assertEquals(2, field.indexedFields.size)
		assertEquals(2, field.fields.count())
		assertEquals(null, field.importedFrom)
	}

	@Test
	@JsName("groupBuilder")
	fun `group builder`() = runTest {
		val field = group(
			"test label",
			0 to label("field 0"),
			1 to input("field 1", Input.Email),
		)

		assertEquals("test label", field.label)
		assertEquals(2, field.indexedFields.size)
		assertEquals(2, field.fields.count())
		assertEquals(null, field.importedFrom)
	}

	@Test
	@JsName("arityBuilder")
	fun `arity builder`() = runTest {
		val field = arity("test label", 1u..3u, label("hello"))

		assertEquals("test label", field.label)
		assertEquals(1u..3u, field.allowed)
		assertEquals("hello", field.child.label)
		assertEquals(3, field.indexedFields.size)

		assertEquals(
			listOf(
				label("hello"),
				label("hello"),
				label("hello"),
			),
			field.fields.toList(),
		)
	}

	@Test
	@JsName("rootIdConversion")
	fun `root id string conversion`() {
		assertEquals(Field.Id.root, Field.Id.fromString(Field.Id.root.toString()))
	}

	@Test
	@JsName("idConversion")
	fun `regular id string conversion`() {
		val single = Field.Id(0)
		val double = Field.Id(0, 1)
		val triple = Field.Id(0, 1, 2)

		assertEquals(single, Field.Id.fromString(single.toString()))
		assertEquals(double, Field.Id.fromString(double.toString()))
		assertEquals(triple, Field.Id.fromString(triple.toString()))
	}

	@Test
	@JsName("rootHeadTail")
	fun `head and tail of the root id`() {
		val root = Field.Id.root

		assertTrue(root.isRoot)
		assertEquals(null, root.headOrNull)
		assertFailsWith<NoSuchElementException> { root.head }
		assertEquals(root, root.tail)
	}

	@Test
	@JsName("headTail")
	fun `head and tail of a regular id`() {
		val id1 = Field.Id(0, 1, 2)
		val id2 = Field.Id(1, 2)
		val id3 = Field.Id(2)
		val root = Field.Id.root

		assertFalse(id1.isRoot)
		assertEquals(0, id1.headOrNull)
		assertEquals(0, id1.head)
		assertEquals(id2, id1.tail)

		assertFalse(id2.isRoot)
		assertEquals(1, id2.headOrNull)
		assertEquals(1, id2.head)
		assertEquals(id3, id2.tail)

		assertFalse(id3.isRoot)
		assertEquals(2, id3.headOrNull)
		assertEquals(2, id3.head)
		assertEquals(root, id3.tail)
	}

	@Test
	@JsName("get")
	fun `get fields`() {
		val firstName = input("First name", Input.Text(maxLength = 50u))
		val firstNames = arity(
			"First name(s)",
			1u..5u,
			firstName,
		)
		val lastName = input("Last name", Input.Text(maxLength = 50u))
		val townName = input("Name", Input.Text(maxLength = 100u))
		val postalCode = input("Postal code", Input.Integer(min = 0, max = 10_000))
		val town = group(
			"Town",
			0 to townName,
			1 to postalCode,
		)
		val field = group(
			"Identity",
			0 to firstNames,
			1 to lastName,
			2 to town
		)

		assertEquals(field, field[Field.Id.root])
		assertEquals(firstNames, field[0])
		assertEquals(firstName, field[0, 0])
		assertEquals(firstName, field[0, 1])
		assertEquals(firstName, field[0, 2])
		assertEquals(firstName, field[0, 3])
		assertEquals(firstName, field[0, 4])
		assertEquals(lastName, field[1])
		assertEquals(town, field[2])
		assertEquals(townName, field[2, 0])
		assertEquals(postalCode, field[2, 1])
	}

}
