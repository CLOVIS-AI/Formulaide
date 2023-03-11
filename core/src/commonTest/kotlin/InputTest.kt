package opensavvy.formulaide.core

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import opensavvy.formulaide.core.data.Email
import opensavvy.formulaide.fake.FakeFiles
import opensavvy.formulaide.test.assertions.shouldBeInvalid
import opensavvy.formulaide.test.assertions.shouldSucceedAnd
import opensavvy.formulaide.test.utils.TestClock.Companion.testClock
import opensavvy.state.outcome.out
import kotlin.js.JsName
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class InputTest {

	@Test
	@JsName("textMaxSize")
	fun `text max size`() = runTest {
		val files = FakeFiles(testClock())

		val text = Input.Text(maxLength = 5u)

		assertEquals(5u, text.effectiveMaxLength)

		text.parse("hello", files).shouldSucceedAnd {
			assertEquals("hello", it)
		}

		shouldBeInvalid(text.parse("too long", files))
		shouldBeInvalid(text.parse("123456", files))
	}

	@Test
	@JsName("integer")
	fun `parse integer`() = runTest {
		val files = FakeFiles(testClock())

		val int = Input.Integer(min = -5, max = 5)

		assertEquals(-5, int.effectiveMin)
		assertEquals(5, int.effectiveMax)

		int.parse("1", files).shouldSucceedAnd {
			assertEquals(1, it)
		}

		int.parse("-5", files).shouldSucceedAnd {
			assertEquals(-5, it)
		}

		int.parse("5", files).shouldSucceedAnd {
			assertEquals(5, it)
		}

		shouldBeInvalid(int.parse("-6", files))
		shouldBeInvalid(int.parse("6", files))
		shouldBeInvalid(int.parse("95", files))
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
		val files = FakeFiles(testClock())
		val bool = Input.Toggle

		bool.parse("true", files).shouldSucceedAnd {
			assertEquals(true, it)
		}

		bool.parse("false", files).shouldSucceedAnd {
			assertEquals(false, it)
		}

		shouldBeInvalid(bool.parse("other", files))
		shouldBeInvalid(bool.parse("something", files))
	}

	@Test
	@JsName("email")
	fun `parse email`() = runTest {
		val files = FakeFiles(testClock())
		val email = Input.Email

		email.parse("my-email@gmail.com", files).shouldSucceedAnd {
			assertEquals(Email("my-email@gmail.com"), it)
		}

		shouldBeInvalid(email.parse("something", files))
	}

	@Test
	@JsName("phone")
	fun `parse phone number`() = runTest {
		val files = FakeFiles(testClock())
		val phone = Input.Phone

		phone.parse("+332345678", files).shouldSucceedAnd {
			assertEquals("+332345678", it)
		}

		shouldBeInvalid(phone.parse("thing", files))
		shouldBeInvalid(phone.parse("123456789123456789123456789", files))
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
