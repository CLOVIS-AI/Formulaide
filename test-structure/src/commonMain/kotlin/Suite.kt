package opensavvy.formulaide.test.structure

import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

@DslMarker
annotation class SuiteDsl

@SuiteDsl
interface Suite {

    @SuiteDsl
    fun suite(
        name: String,
        block: Suite.() -> Unit,
    )

    @SuiteDsl
    fun test(
        name: String,
        context: CoroutineContext = EmptyCoroutineContext,
        block: suspend TestScope.() -> Unit,
    )
}
