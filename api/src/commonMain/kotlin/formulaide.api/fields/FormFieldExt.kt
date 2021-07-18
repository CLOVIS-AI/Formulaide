package formulaide.api.fields

/**
 * A [Sequence] of all the fields that make up this form field, including itself.
 *
 * Use the generated sequence to recursively do some operation on all fields.
 */
fun FormField.fieldMonad(): Sequence<FormField> = when (this) {
	is ShallowFormField.Simple -> sequenceOf(this)
	is ShallowFormField.Union -> options.asSequence().flatMap { it.fieldMonad() } + this
	is ShallowFormField.Composite -> fields.asSequence().flatMap { it.fieldMonad() } + this
	is DeepFormField.Simple -> sequenceOf(this)
	is DeepFormField.Union -> options.asSequence().flatMap { it.fieldMonad() } + this
	is DeepFormField.Composite -> fields.asSequence().flatMap { it.fieldMonad() } + this
}

/**
 * A [Sequence] of all the fields that make up this form root.
 *
 * Use the generated sequence to recursively do some operation on all fields.
 */
fun FormRoot.fieldMonad(): Sequence<FormField> =
	fields.asSequence().flatMap { it.fieldMonad() }
