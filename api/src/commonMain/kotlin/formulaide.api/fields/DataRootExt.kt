package formulaide.api.fields

import formulaide.api.data.Composite
import formulaide.api.types.Ref

/**
 * A [Sequence] of all the fields that make up this data field, including itself.
 *
 * Use the generated sequence to recursively do some operation on all fields.
 */
fun DataField.fieldMonad(): Sequence<DataField> = when (this) {
	is DataField.Union -> options.asSequence().flatMap { it.fieldMonad() } + this
	is DataField.Simple, is DataField.Composite -> sequenceOf(this)
}

/**
 * Loads this [DataField] if it hasn't been loaded yet.
 *
 * This function is not recursive; see [fieldMonad].
 *
 * Parameters [allowNotFound] and [lazy] are passed to [Ref.loadFrom].
 */
fun DataField.load(
	composites: List<Composite>,
	allowNotFound: Boolean = false,
	lazy: Boolean = true,
) = when (this) {
	is DataField.Simple, is DataField.Union -> Unit
	is DataField.Composite -> ref.loadFrom(composites, allowNotFound, lazy)
}
