package opensavvy.formulaide.test.execution

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

interface Suite {

	fun suite(
		name: String,
		block: Suite.() -> Unit,
	)

	@OptIn(ExperimentalCoroutinesApi::class)
	fun test(
		name: String,
		context: CoroutineContext = EmptyCoroutineContext,
		block: suspend TestScope.() -> Unit,
	)

}
