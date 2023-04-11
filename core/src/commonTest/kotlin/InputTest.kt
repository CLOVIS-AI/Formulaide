package opensavvy.formulaide.core

import opensavvy.formulaide.core.data.Email
import opensavvy.formulaide.fake.FakeFiles
import opensavvy.formulaide.test.assertions.shouldBeInvalid
import opensavvy.formulaide.test.assertions.shouldSucceedAnd
import opensavvy.formulaide.test.structure.Suite
import opensavvy.formulaide.test.structure.TestExecutor
import opensavvy.formulaide.test.structure.clock
import opensavvy.state.outcome.out
import kotlin.test.assertEquals

class InputTest : TestExecutor() {

	override fun Suite.register() {
		test("Text max size") {
			val files = FakeFiles(clock)

			val text = Input.Text(maxLength = 5u)

			assertEquals(5u, text.effectiveMaxLength)

			text.parse("hello", files).shouldSucceedAnd {
				assertEquals("hello", it)
			}

			shouldBeInvalid(text.parse("too long", files))
			shouldBeInvalid(text.parse("123456", files))
		}

		test("Parse integer") {
			val files = FakeFiles(clock)

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

		test("Check invalid int range") {
			shouldBeInvalid(out { Input.Integer(min = 6, max = 5) })
			shouldBeInvalid(out { Input.Integer(min = 5, max = 5) })
		}

		test("Parse boolean") {
			val files = FakeFiles(clock)
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

		test("Email") {
			val files = FakeFiles(clock)
			val email = Input.Email

			email.parse("my-email@gmail.com", files).shouldSucceedAnd {
				assertEquals(Email("my-email@gmail.com"), it)
			}

			shouldBeInvalid(email.parse("something", files))
		}

		test("Parse phone number") {
			val files = FakeFiles(clock)
			val phone = Input.Phone

			phone.parse("+332345678", files).shouldSucceedAnd {
				assertEquals("+332345678", it)
			}

			shouldBeInvalid(phone.parse("thing", files))
			shouldBeInvalid(phone.parse("123456789123456789123456789", files))
		}

		test("Int range constructor") {
			assertEquals(Input.Integer(min = 0, max = 5), Input.Integer(0..5))
		}

		test("Long range constructor") {
			assertEquals(Input.Integer(min = 0, max = 5), Input.Integer(0L..5L))
		}

		test("Long range accessor") {
			assertEquals(0L..5L, Input.Integer(0L..5L).effectiveRange)
		}
	}
}
