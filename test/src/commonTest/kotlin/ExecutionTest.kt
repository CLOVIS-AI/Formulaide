package opensavvy.formulaide.test

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import opensavvy.formulaide.test.execution.Executor
import opensavvy.formulaide.test.execution.Suite

@Suppress("unused")
@OptIn(ExperimentalCoroutinesApi::class)
class ExecutionTest : Executor() {

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
