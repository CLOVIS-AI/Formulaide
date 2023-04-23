@file:Suppress("unused")

package opensavvy.formulaide.test.assertions

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import opensavvy.state.failure.Failure
import opensavvy.state.failure.NotFound
import opensavvy.state.failure.Unauthenticated
import opensavvy.state.failure.Unauthorized
import opensavvy.state.outcome.Outcome
import kotlin.contracts.contract
import kotlin.js.JsName
import kotlin.jvm.JvmName

fun <T> shouldSucceed(outcome: Outcome<*, T>): T {
	contract {
		returns() implies (outcome is Outcome.Success<T>)
	}

	withClue({ "Expected a successful result, but failed with ${(outcome as Outcome.Failure).failure}" }) {
		outcome::class shouldBe Outcome.Success::class
	}

	outcome as Outcome.Success // tested by the previous test case

	return outcome.value
}

@JvmName("shouldSucceedWithReceiver")
@JsName("shouldSucceedWithReceiver")
fun <T> Outcome<*, T>.shouldSucceed(): T {
	contract {
		returns() implies (this@shouldSucceed is Outcome.Success<T>)
	}

	return shouldSucceed(this)
}

inline infix fun <T> Outcome<*, T>.shouldSucceedAnd(assertions: (T) -> Unit): T {
	contract {
		returns() implies (this@shouldSucceedAnd is Outcome.Success<T>)
	}

	val value = shouldSucceed(this)

	withClue({ value }) {
		assertions(value)
	}

	return value
}

inline infix fun <T> Outcome<*, T>.shouldSucceedAndSoftly(assertions: (T) -> Unit): T {
	contract {
		returns() implies (this@shouldSucceedAndSoftly is Outcome.Success<T>)
	}

	val value = shouldSucceed(this)

	withClue({ value }) {
		assertSoftly {
			assertions(value)
		}
	}

	return value
}

fun <F : Failure> Outcome<F, *>.shouldFail(): Failure {
	contract {
		returns() implies (this@shouldFail is Outcome.Failure<F>)
	}

	this::class shouldBe Outcome.Failure::class

	this as Outcome.Failure

	return failure
}

infix fun <F : Failure> Outcome<F, *>.shouldFailWithKey(key: Failure.Key) {
	withClue({ "Result: $this" }) {
		withClue({ "Expected to fail with $key" }) {
			shouldFail()
			(this as Outcome.Failure).failure.key shouldBe key
		}
	}
}

fun <F : Failure, T> shouldNotBeAuthenticated(outcome: Outcome<F, T>) = outcome shouldFailWithKey Unauthenticated
fun <F : Failure, T> shouldNotBeAuthorized(outcome: Outcome<F, T>) = outcome shouldFailWithKey Unauthorized
fun <F : Failure, T> shouldNotBeFound(outcome: Outcome<F, T>) = outcome shouldFailWithKey NotFound
