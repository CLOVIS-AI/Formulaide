package opensavvy.formulaide.test.structure

import io.kotest.assertions.withClue
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.coroutineContext
import kotlin.reflect.KProperty

class Setup<out T : Any>(
    val name: String,
    private val finalize: suspend TestScope.(T) -> Unit = {},
    private val block: suspend TestScope.() -> T,
) {

    suspend fun executeIn(scope: TestScope): T = with(scope) {
        val test = coroutineContext[Test]
            ?: error("Cannot execute a setup phase outside of a test (attempted to prepare '$name')")

        @Suppress("UNCHECKED_CAST")
        test.cacheValue(scope, this@Setup) {
            println("Preparing '$name'…")

            val result = block()

            test.registerFinalizer {
                withClue("Finalizing '$name'…") {
                    finalize(result)
                }
            }

            result
        } as T
    }
}

class SetupDelegate<T : Any>(
    private val setup: Setup<T>,
) {

    operator fun getValue(thisRef: Any?, property: KProperty<*>): Setup<T> = setup
}

class SetupProvider<T : Any>(
    private val finalize: suspend TestScope.(T) -> Unit = {},
    private val block: suspend TestScope.() -> T,
) {

    operator fun provideDelegate(thisRef: Any?, property: KProperty<*>) = SetupDelegate(
        Setup(
            property.name,
            finalize,
            block,
        )
    )
}

fun <T : Any> prepared(
    context: CoroutineContext = EmptyCoroutineContext,
    finalize: suspend TestScope.(T) -> Unit = {},
    block: suspend TestScope.() -> T,
): SetupProvider<T> = SetupProvider(finalize) {
    withContext(context) {
        block()
    }
}

suspend fun <T : Any> TestScope.prepare(setup: Setup<T>): T = with(setup) {
    withClue("Preparing '${setup.name}'…") {
        executeIn(this@prepare)
    }
}
