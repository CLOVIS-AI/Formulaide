package opensavvy.formulaide.test.execution

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope

/**
 * Asynchronously creates an instance of `T`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
typealias Factory<T> = suspend TestScope.() -> T
