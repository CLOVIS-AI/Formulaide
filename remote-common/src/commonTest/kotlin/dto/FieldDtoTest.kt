package opensavvy.formulaide.remote.dto

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import opensavvy.formulaide.core.Field.Companion.arity
import opensavvy.formulaide.core.Field.Companion.choice
import opensavvy.formulaide.core.Field.Companion.group
import opensavvy.formulaide.core.Field.Companion.input
import opensavvy.formulaide.core.Field.Companion.label
import opensavvy.formulaide.core.Input
import opensavvy.formulaide.core.Template
import opensavvy.formulaide.remote.dto.FieldDto.Companion.toCore
import opensavvy.formulaide.remote.dto.FieldDto.Companion.toDto
import kotlin.js.JsName
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class FieldDtoTest {

	private fun decodeTemplate(): Template.Version.Ref? = null

	@Test
	@JsName("labelConversion")
	fun `label conversion`() = runTest {
		val field = label("Label")
		assertEquals(field, field.toDto().toCore { decodeTemplate() })
	}

	@Test
	@JsName("arityConversion")
	fun `arity conversion`() = runTest {
		val field = arity("Arity", 0u..2u, label("Label"))
		assertEquals(field, field.toDto().toCore { decodeTemplate() })
	}

	@Test
	@JsName("choiceConversion")
	fun `choice conversion`() = runTest {
		val field = choice(
			"Choice",
			0 to label("Label 1"),
			1 to label("Label 2"),
		)
		assertEquals(field, field.toDto().toCore { decodeTemplate() })
	}

	@Test
	@JsName("groupConversion")
	fun `group conversion`() = runTest {
		val field = group(
			"Group",
			0 to label("Label 1"),
			1 to label("Label 2"),
		)
		assertEquals(field, field.toDto().toCore { decodeTemplate() })
	}

	@Test
	@JsName("inputConversion")
	fun `input conversion`() = runTest {
		val field = input(
			"Input",
			Input.Text(),
		)
		assertEquals(field, field.toDto().toCore { decodeTemplate() })
	}

}
