package opensavvy.formulaide.test.structure

import arrow.core.raise.Raise

/**
 * Fake implementation of [Raise] that throws an [AssertionError].
 */
internal object TestRaise : Raise<Any> {

    override fun raise(r: Any): Nothing {
        throw AssertionError("Unexpected result: $r")
    }
}
