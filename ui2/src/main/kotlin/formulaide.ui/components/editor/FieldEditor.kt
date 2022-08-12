package formulaide.ui.components.editor

import androidx.compose.runtime.*
import formulaide.core.field.Field
import formulaide.ui.components.TextButton
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text

@Composable
fun FieldEditor(
	root: MutableField,
	onReplace: (MutableField) -> Unit,
) {
	var selectedId by remember { mutableStateOf(Field.Id.root) }
	val selected by remember(root) { derivedStateOf { findFieldById(root, selectedId) } }

	FieldSelector(root, selectedId, onSelect = { selectedId = it })
	SingleFieldEditor(
		selectedId,
		selected,
		onReplace = { target, newValue ->
			if (selectedId == Field.Id.root) {
				onReplace(newValue)
			} else {
				val parent = findFieldById(root, selectedId, last = selectedId.parts.size - 1)
				if (parent is MutableField.List)
					parent.field.value = newValue
				if (parent is MutableField.Group || parent is MutableField.Choice)
					(parent.fields as MutableMap)[target.parts.last()] = newValue
			}
		},
		onSelect = { selectedId = it }
	)
}

private fun findFieldById(parent: MutableField, id: Field.Id, depth: Int = 0, last: Int = id.parts.size): MutableField =
	when {
		depth >= last -> parent
		parent is MutableField.List -> findFieldById(parent.field.value, id, depth + 1, last)
		else -> {
			val head = id.parts[depth]
			val child = parent.fields[head]
				?: error("Could not find child of $parent with ID $id, this should not be possible")
			findFieldById(child, id, depth + 1, last)
		}
	}

@Composable
private fun FieldSelector(
	root: MutableField,
	selected: Field.Id,
	onSelect: (Field.Id) -> Unit,
) = Div {
	var parent = root
	SingleFieldSelector(root, onSelect = { onSelect(Field.Id.root) })

	for ((i, fieldId) in selected.parts.withIndex()) {
		val field = parent.fields[fieldId]
			?: error("Could not find child of $parent with ID $fieldId, this should not be possible")

		Text(" › ")
		SingleFieldSelector(field, onSelect = { onSelect(Field.Id(selected.parts.subList(0, i + 1))) })

		parent = field
	}
}

@Composable
private fun SingleFieldSelector(
	field: MutableField,
	onSelect: () -> Unit,
) {
	TextButton(
		onClick = { onSelect() }
	) {
		val name by field.label

		Text(name)
	}
}
