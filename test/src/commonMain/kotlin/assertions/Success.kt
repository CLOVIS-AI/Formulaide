@file:Suppress("unused")

package opensavvy.formulaide.test.assertions

import arrow.core.Either
import arrow.core.continuations.EffectScope
import arrow.core.continuations.either
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import opensavvy.state.Failure
import opensavvy.state.outcome.Outcome
import kotlin.contracts.contract
import kotlin.js.JsName
import kotlin.jvm.JvmName

fun <T> shouldSucceed(outcome: Outcome<T>): T {
	contract {
		returns() implies (outcome is Either.Right<T>)
	}

	withClue({ "Expected a successful result, but failed with ${(outcome as Either.Left).value}" }) {
		outcome::class shouldBe Either.Right::class
	}

	outcome as Either.Right // tested by the previous test case

	return outcome.value
}

@JvmName("shouldSucceedWithReceiver")
@JsName("shouldSucceedWithReceiver")
fun <T> Outcome<T>.shouldSucceed(): T {
	contract {
		returns() implies (this@shouldSucceed is Either.Right<T>)
	}

	return shouldSucceed(this)
}

inline infix fun <T> Outcome<T>.shouldSucceedAnd(assertions: (T) -> Unit): T {
	contract {
		returns() implies (this@shouldSucceedAnd is Either.Right<T>)
	}

	val value = shouldSucceed(this)

	withClue({ value }) {
		assertions(value)
	}

	return value
}

fun <T> Outcome<T>.shouldFail(): Failure {
	contract {
		returns() implies (this@shouldFail is Either.Left<Failure>)
	}

	this::class shouldBe Either.Left::class

	this as Either.Left

	return value
}

infix fun <T> Outcome<T>.shouldFailWith(kind: Failure.Kind) {
	withClue({ "Result: $this" }) {
		withClue({ "Expected to fail with $kind" }) {
			shouldFail()
			kind shouldBe value.kind
		}
	}
}

fun <T> shouldBeInvalid(outcome: Outcome<T>) = outcome shouldFailWith Failure.Kind.Invalid
fun <T> shouldNotBeAuthenticated(outcome: Outcome<T>) = outcome shouldFailWith Failure.Kind.Unauthenticated
fun <T> shouldNotBeAuthorized(outcome: Outcome<T>) = outcome shouldFailWith Failure.Kind.Unauthorized
fun <T> shouldNotBeFound(outcome: Outcome<T>) = outcome shouldFailWith Failure.Kind.NotFound

suspend fun <T> shouldBeInvalid(block: suspend EffectScope<Failure>.() -> T) =
	either { block() } shouldFailWith Failure.Kind.Invalid

suspend fun <T> shouldNotBeAuthenticated(block: suspend EffectScope<Failure>.() -> T) =
	shouldNotBeAuthenticated(either { block() })

suspend fun <T> shouldNotBeAuthorized(block: suspend EffectScope<Failure>.() -> T) =
	shouldNotBeAuthorized(either { block() })

suspend fun <T> shouldNotBeFound(block: suspend EffectScope<Failure>.() -> T) =
	shouldNotBeFound(either { block() })
