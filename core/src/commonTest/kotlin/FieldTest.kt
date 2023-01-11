package opensavvy.formulaide.core

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import opensavvy.formulaide.core.Field.Companion.arity
import opensavvy.formulaide.core.Field.Companion.choice
import opensavvy.formulaide.core.Field.Companion.group
import opensavvy.formulaide.core.Field.Companion.input
import opensavvy.formulaide.core.Field.Companion.label
import kotlin.js.JsName
import kotlin.test.Test
import kotlin.test.assertEquals

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

}
