package opensavvy.formulaide.test.execution

import opensavvy.formulaide.test.structure.TestScope

/**
 * Asynchronously creates an instance of `T`.
 */
typealias Factory<T> = suspend TestScope.() -> T
