package opensavvy.formulaide.core

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import opensavvy.formulaide.core.data.Email
import opensavvy.formulaide.test.assertions.assertInvalid
import opensavvy.formulaide.test.assertions.assertSuccess
import opensavvy.state.outcome.out
import kotlin.js.JsName
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class InputTest {

	@Test
	@JsName("textMaxSize")
	fun `text max size`() = runTest {
		val text = Input.Text(maxLength = 5u)

		assertEquals(5u, text.effectiveMaxLength)

		assertSuccess(text.parse("hello")) {
			assertEquals("hello", it)
		}

		assertInvalid(text.parse("too long"))
		assertInvalid(text.parse("123456"))
	}

	@Test
	@JsName("integer")
	fun `parse integer`() = runTest {
		val int = Input.Integer(min = -5, max = 5)

		assertEquals(-5, int.effectiveMin)
		assertEquals(5, int.effectiveMax)

		assertSuccess(int.parse("1")) {
			assertEquals(1, it)
		}

		assertInvalid(int.parse("-6"))
		assertInvalid(int.parse("95"))
	}

	@Test
	@JsName("invalidIntegerRange")
	fun `check invalid int range`() = runTest {
		assertInvalid(out { Input.Integer(min = 6, max = 5) })
		assertInvalid(out { Input.Integer(min = 5, max = 5) })
	}

	@Test
	@JsName("toggle")
	fun `parse boolean`() = runTest {
		val bool = Input.Toggle

		assertSuccess(bool.parse("true")) {
			assertEquals(true, it)
		}

		assertSuccess(bool.parse("false")) {
			assertEquals(false, it)
		}

		assertInvalid(bool.parse("other"))
		assertInvalid(bool.parse("something"))
	}

	@Test
	@JsName("email")
	fun `parse email`() = runTest {
		val email = Input.Email

		assertSuccess(email.parse("my-email@gmail.com")) {
			assertEquals(Email("my-email@gmail.com"), it)
		}

		assertInvalid(email.parse("something"))
	}

	@Test
	@JsName("phone")
	fun `parse phone number`() = runTest {
		val phone = Input.Phone

		assertSuccess(phone.parse("+332345678")) {
			assertEquals("+332345678", it)
		}

		assertInvalid(phone.parse("thing"))
		assertInvalid(phone.parse("123456789123456789123456789"))
	}
}
