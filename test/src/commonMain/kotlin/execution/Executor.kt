package opensavvy.formulaide.test.execution

/**
 * Declares a test suite.
 *
 * To declare the suite, create a class that extends `Executor` and implement the [register] function.
 */
expect abstract class Executor() {

	abstract fun Suite.register()

}
