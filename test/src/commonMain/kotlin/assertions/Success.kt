@file:Suppress("unused")

package opensavvy.formulaide.test.assertions

import arrow.core.Either
import opensavvy.state.Failure
import opensavvy.state.slice.Slice
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalContracts::class)
inline fun <T> assertSuccess(actual: Slice<T>, assertions: T.() -> Unit = {}): T {
	contract {
		returns() implies (actual is Either.Right<T>)
	}

	assertIs<Either.Right<T>>(actual, (actual as? Either.Left)?.value?.toString())
	assertions(actual.value)

	return actual.value
}

@OptIn(ExperimentalContracts::class)
fun <T> assertFails(actual: Slice<T>, message: String? = null): Failure {
	contract {
		returns() implies (actual is Either.Left<Failure>)
	}

	val fullMessage = buildString {
		append(actual)

		if (message != null)
			append(". $message")
	}

	assertIs<Either.Left<Failure>>(actual, fullMessage)

	return actual.value
}

private fun <T> assertFailureKind(actual: Slice<T>, kind: Failure.Kind) {
	assertFails(actual, "Expected $kind")
	assertEquals(kind, actual.value.kind, "Result: $actual")
}

fun <T> assertInvalid(actual: Slice<T>) = assertFailureKind(actual, Failure.Kind.Invalid)
fun <T> assertUnauthenticated(actual: Slice<T>) = assertFailureKind(actual, Failure.Kind.Unauthenticated)
fun <T> assertUnauthorized(actual: Slice<T>) = assertFailureKind(actual, Failure.Kind.Unauthorized)
fun <T> assertNotFound(actual: Slice<T>) = assertFailureKind(actual, Failure.Kind.NotFound)
