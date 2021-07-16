package formulaide.api.fields

/**
 * A [Sequence] of all the fields that make up this data field, including itself.
 *
 * Use the generated sequence to recursively do some operation on all fields.
 */
fun DataField.fieldMonad(): Sequence<DataField> = when (this) {
	is DataField.Union -> options.asSequence().flatMap { it.fieldMonad() } + this
	is DataField.Simple, is DataField.Composite -> sequenceOf(this)
}
