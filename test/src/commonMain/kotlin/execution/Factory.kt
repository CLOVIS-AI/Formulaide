package opensavvy.formulaide.test.execution

import kotlinx.coroutines.test.TestScope

/**
 * Asynchronously creates an instance of `T`.
 */
typealias Factory<T> = suspend TestScope.() -> T
