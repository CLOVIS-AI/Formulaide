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
fun <T> assertFails(actual: Slice<T>): Failure {
	contract {
		returns() implies (actual is Either.Left<Failure>)
	}

	assertIs<Either.Left<Failure>>(actual, (actual as? Either.Right)?.value?.toString())

	return actual.value
}

private fun <T> assertFailureKind(actual: Slice<T>, kind: Failure.Kind) {
	assertFails(actual)
	assertEquals(kind, actual.value.kind, "Result: $actual")
}

fun <T> assertInvalid(actual: Slice<T>) = assertFailureKind(actual, Failure.Kind.Invalid)
fun <T> assertUnauthenticated(actual: Slice<T>) = assertFailureKind(actual, Failure.Kind.Unauthenticated)
fun <T> assertUnauthorized(actual: Slice<T>) = assertFailureKind(actual, Failure.Kind.Unauthorized)
fun <T> assertNotFound(actual: Slice<T>) = assertFailureKind(actual, Failure.Kind.NotFound)
