package opensavvy.formulaide.test.execution

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import java.util.stream.Stream
import kotlin.coroutines.CoroutineContext

actual abstract class Executor actual constructor(private val name: String) {

	actual abstract fun Suite.register()

	@Suppress("FunctionName")
	@TestFactory
	fun `dynamic tests`(): Stream<out DynamicNode> {
		val suite = JvmSuite().apply { register() }

		return Stream.of(DynamicContainer.dynamicContainer(name, suite.nodes))
	}

}

private class JvmSuite : Suite {
	val nodes = ArrayList<DynamicNode>()

	override fun suite(name: String, block: Suite.() -> Unit) {
		val child = JvmSuite().apply(block)

		nodes += DynamicContainer.dynamicContainer(name, child.nodes)
	}

	@OptIn(ExperimentalCoroutinesApi::class)
	override fun test(name: String, context: CoroutineContext, block: suspend TestScope.() -> Unit) {
		nodes += DynamicTest.dynamicTest(name) {
			runTest(context) {
				block()
			}
		}
	}

}
