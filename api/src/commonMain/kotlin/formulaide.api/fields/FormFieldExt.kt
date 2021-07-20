package formulaide.api.fields

/**
 * A [Sequence] of all the fields that make up this form field, including itself.
 *
 * Use the generated sequence to recursively do some operation on all fields.
 */
fun FormField.fieldMonad(): Sequence<FormField> {
	val self = sequenceOf(this)

	return when (this) {
		is ShallowFormField.Simple -> self
		is ShallowFormField.Union -> self + options.asSequence().flatMap { it.fieldMonad() }
		is ShallowFormField.Composite -> self + fields.asSequence().flatMap { it.fieldMonad() }
		is DeepFormField.Simple -> self
		is DeepFormField.Union -> self + options.asSequence().flatMap { it.fieldMonad() }
		is DeepFormField.Composite -> self + fields.asSequence().flatMap { it.fieldMonad() }
	}
}

/**
 * A [Sequence] of all the fields that make up this form root.
 *
 * Use the generated sequence to recursively do some operation on all fields.
 */
fun FormRoot.fieldMonad(): Sequence<FormField> =
	fields.asSequence().flatMap { it.fieldMonad() }
