package opensavvy.formulaide.test.structure

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay

@Suppress("unused")
class ExecutionTest : TestExecutor() {

	override fun Suite.register() {
		suite("test suite") {
			test("example") {
				true shouldBe true
			}
		}

		suite("first suite") {
			test("first example") {
				delay(1000)
				true shouldBe true
			}

			test("second example") {
				withClue("it shouldn't be false!") {
					true shouldBe true
				}
			}
		}

		suite("second suite") {
			suite("merged suite") {
				test("this is another test") {
					true shouldBe true
				}
			}
		}
	}

}
