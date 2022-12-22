package opensavvy.formulaide.test.assertions

import kotlin.test.assertFalse

fun <T> assertNotContains(container: Iterable<T>, item: T) {
	assertFalse(item in container, "$item was not expected to be a part of $container")
}
