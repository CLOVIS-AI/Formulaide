package formulaide.ui.components.editor

import androidx.compose.runtime.*
import opensavvy.formulaide.core.Field

@Composable
fun FieldEditor(
	root: MutableField,
	onReplace: (MutableField) -> Unit,
) {
	var selectedId by remember { mutableStateOf(Field.Id.root) }
	val selected by remember(root) { derivedStateOf { findFieldById(root, selectedId) } }

	FieldSelector(root, selectedId, onSelect = { selectedId = it })
	SingleFieldEditor(
		root,
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

fun findFieldById(parent: MutableField, id: Field.Id, depth: Int = 0, last: Int = id.parts.size): MutableField =
	when {
		depth >= last -> parent
		parent is MutableField.List -> findFieldById(parent.field.value, id, depth + 1, last)
		else -> {
			val head = id.parts[depth]
			val child = parent.fields[head]
				?: error("Could not find child of $parent with ID $id, this should not be possible.")
			findFieldById(child, id, depth + 1, last)
		}
	}
