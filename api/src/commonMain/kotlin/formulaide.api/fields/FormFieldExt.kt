package formulaide.api.fields

/**
 * A [Sequence] of all the fields that make up this form field, including itself.
 *
 * Use the generated sequence to recursively do some operation on all fields.
 */
fun FormField.asSequence(): Sequence<FormField> {
	val self = sequenceOf(this)

	return when (this) {
		is ShallowFormField.Simple -> self
		is ShallowFormField.Union -> self + options.asSequence().flatMap { it.asSequence() }
		is ShallowFormField.Composite -> self + fields.asSequence().flatMap { it.asSequence() }
		is DeepFormField.Simple -> self
		is DeepFormField.Union -> self + options.asSequence().flatMap { it.asSequence() }
		is DeepFormField.Composite -> self + fields.asSequence().flatMap { it.asSequence() }
	}
}

/**
 * A [Sequence] of all the fields that make up this form root.
 *
 * Use the generated sequence to recursively do some operation on all fields.
 */
fun FormRoot.asSequence(): Sequence<FormField> =
	fields.asSequence().flatMap { it.asSequence() }

fun FormField.asSequenceWithKey(key: String = id): Sequence<Pair<String, FormField>> {
	val self = sequenceOf(key to this)

	return when (this) {
		is ShallowFormField.Simple -> self
		is ShallowFormField.Union -> self + options.asSequence()
			.flatMap { it.asSequenceWithKey("$key:${it.id}") }
		is ShallowFormField.Composite -> self + fields.asSequence()
			.flatMap { it.asSequenceWithKey("$key:${it.id}") }
		is DeepFormField.Simple -> self
		is DeepFormField.Union -> self + options.asSequence()
			.flatMap { it.asSequenceWithKey("$key:${it.id}") }
		is DeepFormField.Composite -> self + fields.asSequence()
			.flatMap { it.asSequenceWithKey("$key:${it.id}") }
	}
}

fun FormRoot.asSequenceWithKey(): Sequence<Pair<String, FormField>> =
	fields.asSequence().flatMap { it.asSequenceWithKey() }
