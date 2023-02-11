package opensavvy.formulaide.test

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import opensavvy.formulaide.test.execution.Executor
import opensavvy.formulaide.test.execution.Suite
import kotlin.test.assertTrue

@Suppress("unused")
@OptIn(ExperimentalCoroutinesApi::class)
class ExecutionTest : Executor() {

	override fun Suite.register() {
		suite("test suite") {
			test("example") {
				assertTrue(true)
			}
		}

		suite("first suite") {
			test("first example") {
				delay(1000)
				assertTrue(true)
			}

			test("second example") {
				assertTrue(true)
			}
		}

		suite("second suite") {
			suite("merged suite") {
				test("this is another test") {
					assertTrue(true)
				}
			}
		}
	}

}
