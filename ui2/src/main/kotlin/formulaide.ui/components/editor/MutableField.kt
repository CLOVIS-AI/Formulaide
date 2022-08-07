package formulaide.ui.components.editor

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.toMutableStateMap
import formulaide.core.field.Field
import formulaide.core.field.InputConstraints
import formulaide.core.field.LocalFieldId

/**
 * Mutable implementation of [Field].
 *
 * See [Field]'s documentation to learn more about each field.
 *
 * This type is compose-aware: it doesn't need to be wrapped inside a [State].
 */
sealed class MutableField(label: String, source: Pair<Field.Container, Field.Id>?) {

	val label = mutableStateOf(label)

	abstract val fields: Map<Int, MutableField>

	val source = mutableStateOf(source)

	abstract fun toField(): Field

	class Label(
		label: String,
		source: Pair<Field.Container, Field.Id>?,
	) : MutableField(label, source) {
		override val fields: Map<Int, MutableField> = emptyMap()

		override fun toField() = Field.Label(
			label.value,
			source.value,
		)
	}

	class Input(
		label: String,
		input: InputConstraints,
		source: Pair<Field.Container, Field.Id>?,
	) : MutableField(label, source) {
		var input = mutableStateOf(input)
		override val fields: Map<Int, MutableField> = emptyMap()

		override fun toField() = Field.Input(
			label.value,
			input.value,
			source.value,
		)
	}

	class Choice(
		label: String,
		fields: Iterable<Pair<LocalFieldId, MutableField>>,
		source: Pair<Field.Container, Field.Id>?,
	) : MutableField(label, source) {
		override val fields = fields.toMutableStateMap()

		override fun toField() = Field.Choice(
			label.value,
			fields.mapValues { (_, v) -> v.toField() },
			source.value,
		)
	}

	class Group(
		label: String,
		fields: Iterable<Pair<LocalFieldId, MutableField>>,
		source: Pair<Field.Container, Field.Id>?,
	) : MutableField(label, source) {
		override val fields = fields.toMutableStateMap()

		override fun toField() = Field.Group(
			label.value,
			fields.mapValues { (_, v) -> v.toField() },
			source.value,
		)
	}

	class List(
		label: String,
		field: MutableField,
		allowed: UIntRange,
		source: Pair<Field.Container, Field.Id>?,
	) : MutableField(label, source) {
		val field = mutableStateOf(field)
		val min = mutableStateOf(allowed.first)
		val max = mutableStateOf(allowed.last)
		override val fields: Map<Int, MutableField>
			get() = List(max.value.toInt()) { it to this.field.value }.toMap()

		override fun toField() = Field.List(
			label.value,
			field.value.toField(),
			min.value..max.value,
			source.value,
		)
	}

	companion object {
		private fun Map<Int, Field>.toMutable() = this
			.asSequence()
			.map { (localId, field) -> localId to field.toMutable() }
			.toList()

		fun Field.toMutable(): MutableField = when (this) {
			is Field.Choice -> Choice(label, indexedFields.toMutable(), sourceId)
			is Field.Group -> Group(label, indexedFields.toMutable(), sourceId)
			is Field.Input -> Input(label, input, sourceId)
			is Field.Label -> Label(label, sourceId)
			is Field.List -> List(label, field.toMutable(), allowed, sourceId)
		}
	}
}
