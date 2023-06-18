@file:Suppress("unused")

package opensavvy.formulaide.test.assertions

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import opensavvy.formulaide.core.data.StandardNotFound
import opensavvy.formulaide.core.data.StandardUnauthenticated
import opensavvy.formulaide.core.data.StandardUnauthorized
import opensavvy.state.outcome.Outcome
import kotlin.contracts.contract
import kotlin.js.JsName
import kotlin.jvm.JvmName
import kotlin.reflect.KClass

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

fun <F> Outcome<F, *>.shouldFail(): F {
	contract {
		returns() implies (this@shouldFail is Outcome.Failure<F>)
	}

	this::class shouldBe Outcome.Failure::class

	this as Outcome.Failure

	return failure
}

infix fun <F : Any> Outcome<F, *>.shouldFailWith(failure: F) {
	withClue({ "Result: $this" }) {
		withClue({ "Expected to fail with $failure" }) {
			val value = this
			value.shouldFail()
			value.failure shouldBe failure
		}
	}
}

inline fun <reified F : Any> Outcome<*, *>.shouldFailWithType() {
	withClue({ "Result: $this" }) {
		withClue({ "Expected to fail with ${F::class}" }) {
			val value = this
			value.shouldFail()
			value.failure.shouldBeInstanceOf<F>()
		}
	}
}

inline infix fun <reified F : Any> Outcome<*, *>.shouldFailWithType(@Suppress("UNUSED_PARAMETER") type: KClass<F>) {
	shouldFailWithType<F>()
}

fun <F : Any, T> shouldNotBeAuthenticated(outcome: Outcome<F, T>) = outcome shouldFailWithType StandardUnauthenticated::class
fun <F : Any, T> shouldNotBeAuthorized(outcome: Outcome<F, T>) = outcome shouldFailWithType StandardUnauthorized::class
fun <F : Any, T> shouldNotBeFound(outcome: Outcome<F, T>) = outcome shouldFailWithType StandardNotFound::class
