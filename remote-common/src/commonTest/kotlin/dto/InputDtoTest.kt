package opensavvy.formulaide.remote.dto

import opensavvy.formulaide.core.Input
import opensavvy.formulaide.remote.dto.InputDto.Companion.toCore
import opensavvy.formulaide.remote.dto.InputDto.Companion.toDto
import kotlin.js.JsName
import kotlin.test.Test
import kotlin.test.assertEquals

class InputDtoTest {

	@Test
	@JsName("baseText")
	fun `base text`() {
		val input = Input.Text()
		assertEquals(input, input.toDto().toCore())
	}

	@Test
	@JsName("maxLengthText")
	fun `text with maximum length`() {
		val input = Input.Text(maxLength = 5u)
		assertEquals(input, input.toDto().toCore())
	}

	@Test
	@JsName("baseInt")
	fun `base integer`() {
		val input = Input.Integer()
		assertEquals(input, input.toDto().toCore())
	}

	@Test
	@JsName("minMaxInt")
	fun `integer with range`() {
		val input = Input.Integer(min = 2, max = 3)
		assertEquals(input, input.toDto().toCore())
	}

	@Test
	fun toggle() {
		val input = Input.Toggle
		assertEquals(input, input.toDto().toCore())
	}

	@Test
	fun email() {
		val input = Input.Email
		assertEquals(input, input.toDto().toCore())
	}

	@Test
	fun phone() {
		val input = Input.Phone
		assertEquals(input, input.toDto().toCore())
	}

	@Test
	fun date() {
		val input = Input.Date
		assertEquals(input, input.toDto().toCore())
	}

	@Test
	fun time() {
		val input = Input.Time
		assertEquals(input, input.toDto().toCore())
	}

}
