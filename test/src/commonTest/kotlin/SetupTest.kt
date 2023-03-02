package opensavvy.formulaide.test

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.shouldBe
import opensavvy.formulaide.test.execution.Executor
import opensavvy.formulaide.test.execution.Suite
import opensavvy.formulaide.test.execution.prepare
import opensavvy.formulaide.test.execution.prepared

@Suppress("unused")
class SetupTest : Executor() {

	override fun Suite.register() {
		suite("Resource initialization") {

			val database by prepared(
				finalize = {
					it.shouldBeEmpty()
				}
			) {
				HashMap<Int, String>()
			}

			val prepareData by prepared(
				finalize = {
					it.remove(1)
					it.remove(2)
				}
			) {
				val data = prepare(database)

				data[1] = "one"
				data[2] = "two"

				data
			}

			test("An empty database is empty") {
				val data = prepare(database)

				data shouldBe emptyMap()
			}

			test("The data was properly added") {
				val data = prepare(database)
				prepare(prepareData)

				assertSoftly {
					data[1] shouldBe "one"
					data[2] shouldBe "two"
				}
			}
		}
	}
}
