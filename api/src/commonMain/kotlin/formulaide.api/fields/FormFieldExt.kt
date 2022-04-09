package formulaide.api.fields

/**
 * A [Sequence] of all the fields that make up this form field, including itself.
 *
 * Use the generated sequence to recursively do some operation on all fields.
 */
fun FormField.asSequence(checkArity: Boolean = false): Sequence<FormField> {
	val results = sequence {
		val self = sequenceOf(this@asSequence)

		while (true) {
			val result = when (this@asSequence) {
				is ShallowFormField.Simple -> self
				is ShallowFormField.Union -> self + options.asSequence().flatMap { it.asSequence() }
				is ShallowFormField.Composite -> self + fields.asSequence().flatMap { it.asSequence() }
				is DeepFormField.Simple -> self
				is DeepFormField.Union -> self + options.asSequence().flatMap { it.asSequence() }
				is DeepFormField.Composite -> self + fields.asSequence().flatMap { it.asSequence() }
			}

			yield(result)
		}
	}

	return when (checkArity) {
		true -> results.take(arity.max).flatten()
		false -> results.first()
	}
}

/**
 * A [Sequence] of all the fields that make up this form root.
 *
 * Use the generated sequence to recursively do some operation on all fields.
 */
fun FormRoot.asSequence(checkArity: Boolean = false): Sequence<FormField> =
	fields.asSequence().flatMap { it.asSequence(checkArity) }

fun FormField.asSequenceWithKey(key: String = id, checkArity: Boolean = false): Sequence<Pair<String, FormField>> {
	val results = sequence {
		var i = 0
		while (true) {
			val currentKey = if (checkArity) "$key:$i" else key

			val self = sequenceOf(currentKey to this@asSequenceWithKey)

			val result = when (this@asSequenceWithKey) {
				is ShallowFormField.Simple -> self
				is ShallowFormField.Union -> self + options.asSequence()
					.flatMap { it.asSequenceWithKey("$currentKey:${it.id}") }
				is ShallowFormField.Composite -> self + fields.asSequence()
					.flatMap { it.asSequenceWithKey("$currentKey:${it.id}") }
				is DeepFormField.Simple -> self
				is DeepFormField.Union -> self + options.asSequence()
					.flatMap { it.asSequenceWithKey("$currentKey:${it.id}") }
				is DeepFormField.Composite -> self + fields.asSequence()
					.flatMap { it.asSequenceWithKey("$currentKey:${it.id}") }
			}

			yield(result)
			i++
		}
	}

	return when (checkArity) {
		true -> results.take(arity.max).flatten()
		false -> results.first()
	}
}

fun FormRoot.asSequenceWithKey(checkArity: Boolean = false): Sequence<Pair<String, FormField>> =
	fields.asSequence().flatMap { it.asSequenceWithKey(checkArity = checkArity) }
