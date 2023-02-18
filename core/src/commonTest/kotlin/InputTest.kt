package opensavvy.formulaide.core

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import opensavvy.formulaide.core.data.Email
import opensavvy.formulaide.test.assertions.shouldBeInvalid
import opensavvy.formulaide.test.assertions.shouldSucceedAnd
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

		text.parse("hello").shouldSucceedAnd {
			assertEquals("hello", it)
		}

		shouldBeInvalid(text.parse("too long"))
		shouldBeInvalid(text.parse("123456"))
	}

	@Test
	@JsName("integer")
	fun `parse integer`() = runTest {
		val int = Input.Integer(min = -5, max = 5)

		assertEquals(-5, int.effectiveMin)
		assertEquals(5, int.effectiveMax)

		int.parse("1").shouldSucceedAnd {
			assertEquals(1, it)
		}

		int.parse("-5").shouldSucceedAnd {
			assertEquals(-5, it)
		}

		int.parse("5").shouldSucceedAnd {
			assertEquals(5, it)
		}

		shouldBeInvalid(int.parse("-6"))
		shouldBeInvalid(int.parse("6"))
		shouldBeInvalid(int.parse("95"))
	}

	@Test
	@JsName("invalidIntegerRange")
	fun `check invalid int range`() = runTest {
		shouldBeInvalid(out { Input.Integer(min = 6, max = 5) })
		shouldBeInvalid(out { Input.Integer(min = 5, max = 5) })
	}

	@Test
	@JsName("toggle")
	fun `parse boolean`() = runTest {
		val bool = Input.Toggle

		bool.parse("true").shouldSucceedAnd {
			assertEquals(true, it)
		}

		bool.parse("false").shouldSucceedAnd {
			assertEquals(false, it)
		}

		shouldBeInvalid(bool.parse("other"))
		shouldBeInvalid(bool.parse("something"))
	}

	@Test
	@JsName("email")
	fun `parse email`() = runTest {
		val email = Input.Email

		email.parse("my-email@gmail.com").shouldSucceedAnd {
			assertEquals(Email("my-email@gmail.com"), it)
		}

		shouldBeInvalid(email.parse("something"))
	}

	@Test
	@JsName("phone")
	fun `parse phone number`() = runTest {
		val phone = Input.Phone

		phone.parse("+332345678").shouldSucceedAnd {
			assertEquals("+332345678", it)
		}

		shouldBeInvalid(phone.parse("thing"))
		shouldBeInvalid(phone.parse("123456789123456789123456789"))
	}

	@Test
	@JsName("intRangeConstructor")
	fun `int range constructor`() {
		assertEquals(Input.Integer(min = 0, max = 5), Input.Integer(0..5))
	}

	@Test
	@JsName("longRangeConstructor")
	fun `long range constructor`() {
		assertEquals(Input.Integer(min = 0, max = 5), Input.Integer(0L..5L))
	}

	@Test
	@JsName("longRangeAccessor")
	fun `long range accessor`() {
		assertEquals(0L..5L, Input.Integer(0L..5L).effectiveRange)
	}

}
