package opensavvy.formulaide.test.execution

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.TestScope
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

interface Suite {

	fun suite(
		name: String,
		block: Suite.() -> Unit,
	)

	fun test(
		name: String,
		context: CoroutineContext = EmptyCoroutineContext,
		block: suspend TestScope.() -> Unit,
	)

}

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
