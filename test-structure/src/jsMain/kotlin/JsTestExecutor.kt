package opensavvy.formulaide.test.structure

import kotlin.coroutines.CoroutineContext
import kotlin.test.assertTrue


@JsModule("kotlin-test")
@JsNonModule
private external val kTest: dynamic

actual abstract class TestExecutor {

    init {
        kTest.kotlin.test.suite(this::class.simpleName, false) {
            JsSuite.register()
        }
    }

    actual abstract fun Suite.register()

    // To be loaded by the JS runner, at least one regular test needs to be defined…
    // This test is useless but ensures at least one regular test exists
    @kotlin.test.Test
    fun marker() {
        assertTrue(true)
    }

}

private object JsSuite : Suite {
    override fun suite(name: String, block: Suite.() -> Unit) {
        kTest.kotlin.test.suite(name, false) {
            this.block()
        }
    }

    override fun test(name: String, context: CoroutineContext, block: suspend TestScope.() -> Unit) {
        println("Registering test '$name'")
        kTest.kotlin.test.test(name, false) {
            runTestEnhanced(name, context, block)
        }
    }
}
