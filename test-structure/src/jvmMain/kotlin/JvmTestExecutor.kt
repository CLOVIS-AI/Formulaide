package opensavvy.formulaide.test.structure

import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import java.util.stream.Stream
import kotlin.coroutines.CoroutineContext

actual abstract class TestExecutor {

    actual abstract fun Suite.register()

    @Suppress("FunctionName")
    @TestFactory
    fun `dynamic tests`(): Stream<out DynamicNode> {
        val suite = JvmSuite().apply { register() }

        return suite.nodes.stream()
    }

}

private class JvmSuite : Suite {
    val nodes = ArrayList<DynamicNode>()

    override fun suite(name: String, block: Suite.() -> Unit) {
        val child = JvmSuite().apply(block)

        nodes += DynamicContainer.dynamicContainer(name, child.nodes)
    }

    override fun test(name: String, context: CoroutineContext, block: suspend TestScope.() -> Unit) {
        nodes += DynamicTest.dynamicTest(name) {
            runTestEnhanced(name, context, block)
        }
    }
}
