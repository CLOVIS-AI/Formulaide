package opensavvy.formulaide.test.cases

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import opensavvy.backbone.Backbone

/**
 * Generic test case implementation.
 *
 * This interface is used to declare utilities necessary to test [Service].
 */
interface TestCase<Service : Backbone<*>> {

	/**
	 * Creates a new instance of [Service].
	 *
	 * The created instance may or may not be in a clean state
	 * (e.g. it may or may not be empty).
	 *
	 * @param foreground The scope in which the SUT may create new coroutines,
	 * the test will wait for all of them to finish.
	 * @param background The scope in which the dependencies of the SUT may create new coroutines,
	 * the test will not wait for them to finish.
	 */
	suspend fun new(
		foreground: CoroutineScope,
		background: CoroutineScope,
	): Service

	/**
	 * Creates a new instance of [Service].
	 *
	 * This function is a shortcut to extract the `foreground` and `background` arguments of its overload
	 * from the [TestScope] implementation.
	 *
	 * The foreground scope is [TestScope], the background scope is [TestScope.backgroundScope].
	 */
	@OptIn(ExperimentalCoroutinesApi::class)
	suspend fun TestScope.new() = new(
		this,
		backgroundScope,
	)
}
