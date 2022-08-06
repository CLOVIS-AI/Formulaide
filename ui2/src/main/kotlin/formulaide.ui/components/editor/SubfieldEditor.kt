package formulaide.ui.components.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import formulaide.core.field.InputConstraints
import formulaide.ui.components.TextButton
import org.jetbrains.compose.web.dom.Text

@Composable
fun SubfieldEditor(field: MutableField, focused: Boolean) {
	if (field is MutableField.Group || field is MutableField.Choice) {
		val fields = field.fields as SnapshotStateMap<Int, MutableField>

		for ((id, subfield) in field.fields) {
			key(id) {
				FieldEditor(subfield, onReplace = { fields[id] = it })
			}
		}

		if (field.source.value == null && focused)
			TextButton(
				onClick = {
					val id = fields.keys.maxOrNull()?.plus(1) ?: 0
					fields[id] = MutableField.Input("", InputConstraints.Text(), null)
				}
			) {
				Text(if (field is MutableField.Group) "Ajouter un champ" else "Ajouter une option")
			}
	}

	if (field is MutableField.List) {
		var subfield by field.field
		FieldEditor(subfield, onReplace = { subfield = it })
	}
}
