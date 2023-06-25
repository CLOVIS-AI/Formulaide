package opensavvy.formulaide.core

import opensavvy.formulaide.core.data.Email
import opensavvy.formulaide.fake.FakeFiles
import opensavvy.formulaide.test.assertions.shouldFailWithType
import opensavvy.formulaide.test.assertions.shouldSucceedAnd
import opensavvy.formulaide.test.structure.Suite
import opensavvy.formulaide.test.structure.TestExecutor
import opensavvy.formulaide.test.structure.clock
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

			text.parse("too long", files) shouldFailWithType Input.Failures.Parsing::class
			text.parse("123456", files) shouldFailWithType Input.Failures.Parsing::class
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

			int.parse("-6", files) shouldFailWithType Input.Failures.Parsing::class
			int.parse("6", files) shouldFailWithType Input.Failures.Parsing::class
			int.parse("95", files) shouldFailWithType Input.Failures.Parsing::class
		}

		test("Check invalid int range") {
			Input.integer(min = 6, max = 5) shouldFailWithType Input.Failures.Creating::class
			Input.integer(min = 5, max = 5) shouldFailWithType Input.Failures.Creating::class
		}

		test("Parse boolean") {
			val files = FakeFiles(clock)
			val bool = Input.Toggle

			Input.Toggle.parse("true", files).shouldSucceedAnd {
				assertEquals(true, it)
			}

			Input.Toggle.parse("false", files).shouldSucceedAnd {
				assertEquals(false, it)
			}

			Input.Toggle.parse("other", files) shouldFailWithType Input.Failures.Parsing::class
			Input.Toggle.parse("something", files) shouldFailWithType Input.Failures.Parsing::class
		}

		test("Email") {
			val files = FakeFiles(clock)
			val email = Input.Email

			Input.Email.parse("my-email@gmail.com", files).shouldSucceedAnd {
				assertEquals(Email("my-email@gmail.com"), it)
			}

			Input.Email.parse("something", files) shouldFailWithType Input.Failures.Parsing::class
		}

		test("Parse phone number") {
			val files = FakeFiles(clock)
			val phone = Input.Phone

			Input.Phone.parse("+332345678", files).shouldSucceedAnd {
				assertEquals("+332345678", it)
			}

			Input.Phone.parse("thing", files) shouldFailWithType Input.Failures.Parsing::class
			Input.Phone.parse("123456789123456789123456789", files) shouldFailWithType Input.Failures.Parsing::class
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
