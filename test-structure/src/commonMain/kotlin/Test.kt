package opensavvy.formulaide.test.structure

import arrow.core.raise.Raise
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.js.JsName

@JsName("TestImpl")
internal class Test(val name: String) : AbstractCoroutineContextElement(Test) {

    private val cacheLock = Mutex()
    private val cache = HashMap<Any, Any>()

    private val finalizerLock = Mutex()
    private val finalizers = ArrayList<suspend TestScope.() -> Unit>()

    suspend fun cacheValue(scope: TestScope, key: Any, compute: suspend TestScope.() -> Any) =
        cacheLock.withLock { cache[key] } ?: run {
            val result = with(scope) {
                compute()
            }

            cacheLock.withLock { cache[key] = result }

            result
        }

    suspend fun registerFinalizer(block: suspend TestScope.() -> Unit) = finalizerLock.withLock {
        finalizers.add(block)
    }

    suspend fun TestScope.executeFinalizers() = finalizerLock.withLock {
        for (finalizer in finalizers.asReversed()) {
            finalizer()
        }
    }

    companion object : CoroutineContext.Key<Test>
}

private class TestScopeImpl(
        private val scope: kotlinx.coroutines.test.TestScope
) : TestScope, Raise<Any> by TestRaise {
    override val testScope: CoroutineScope
        get() = scope

    override val backgroundScope: CoroutineScope
        get() = scope.backgroundScope

    override val scheduler: TestCoroutineScheduler
        get() = scope.testScheduler

}

fun runTestEnhanced(name: String, context: CoroutineContext, block: suspend TestScope.() -> Unit): TestResult {
    val test = Test(name)

    return runTest(context + test) {
        with(TestScopeImpl(this)) {
            println("> Test “$name”")
            block()

            println("> Test cleanup")
            with(test) { executeFinalizers() }
        }
    }
}
