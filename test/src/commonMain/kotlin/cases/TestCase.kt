package opensavvy.formulaide.test.cases

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
	 */
	suspend fun new(): Service

}
