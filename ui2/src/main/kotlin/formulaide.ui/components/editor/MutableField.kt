package formulaide.ui.components.editor

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.toMutableStateMap
import opensavvy.formulaide.core.Field
import opensavvy.formulaide.core.InputConstraints
import opensavvy.formulaide.core.Template

/**
 * Mutable implementation of [Field].
 *
 * See [Field]'s documentation to learn more about each field.
 *
 * This type is compose-aware: it doesn't need to be wrapped inside a [State].
 */
sealed class MutableField(
	label: String,
	val importedFrom: Template.Version.Ref?,
) {

	val label = mutableStateOf(label)

	abstract val fields: Map<Int, MutableField>

	abstract fun toField(): Field

	class Label(
		label: String,
		importedFrom: Template.Version.Ref?,
	) : MutableField(label, importedFrom) {
		override val fields: Map<Int, MutableField> = emptyMap()

		override fun toField() = Field.Label(
			label.value,
			importedFrom,
		)
	}

	class Input(
		label: String,
		input: InputConstraints,
		importedFrom: Template.Version.Ref?,
	) : MutableField(label, importedFrom) {
		var input = mutableStateOf(input)
		override val fields: Map<Int, MutableField> = emptyMap()

		override fun toField() = Field.Input(
			label.value,
			input.value,
			importedFrom,
		)
	}

	class Choice(
		label: String,
		fields: Iterable<Pair<Int, MutableField>>,
		importedFrom: Template.Version.Ref?,
	) : MutableField(label, importedFrom) {
		override val fields = fields.toMutableStateMap()

		override fun toField() = Field.Choice(
			label.value,
			fields.mapValues { (_, v) -> v.toField() },
			importedFrom,
		)
	}

	class Group(
		label: String,
		fields: Iterable<Pair<Int, MutableField>>,
		importedFrom: Template.Version.Ref?,
	) : MutableField(label, importedFrom) {
		override val fields = fields.toMutableStateMap()

		override fun toField() = Field.Group(
			label.value,
			fields.mapValues { (_, v) -> v.toField() },
			importedFrom,
		)
	}

	class List(
		label: String,
		field: MutableField,
		allowed: UIntRange,
		importedFrom: Template.Version.Ref?,
	) : MutableField(label, importedFrom) {
		val field = mutableStateOf(field)
		val min = mutableStateOf(allowed.first)
		val max = mutableStateOf(allowed.last)
		override val fields: Map<Int, MutableField>
			get() = List(max.value.toInt()) { it to this.field.value }.toMap()

		override fun toField() = Field.Arity(
			label.value,
			field.value.toField(),
			min.value..max.value,
			importedFrom,
		)
	}

	companion object {
		private fun Map<Int, Field>.toMutable() = this
			.asSequence()
			.map { (localId, field) -> localId to field.toMutable() }
			.toList()

		fun Field.toMutable(): MutableField = when (this) {
			is Field.Choice -> Choice(label, indexedFields.toMutable(), importedFrom)
			is Field.Group -> Group(label, indexedFields.toMutable(), importedFrom)
			is Field.Input -> Input(label, input, importedFrom)
			is Field.Label -> Label(label, importedFrom)
			is Field.Arity -> List(label, child.toMutable(), allowed, importedFrom)
		}
	}
}
