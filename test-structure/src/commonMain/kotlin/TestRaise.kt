package opensavvy.formulaide.test.structure

import arrow.core.continuations.EffectScope

/**
 * Fake implementation of [EffectScope] that throws an [AssertionError].
 */
internal object TestRaise : EffectScope<Any> {

    @Suppress("unused") // <B> is necessary to match the parent's signature, even if we don't use it
    override suspend fun <B> shift(r: Any): Nothing {
        throw AssertionError("Unexpected result: $r")
    }
}
