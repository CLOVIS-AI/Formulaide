package formulaide.ui.components.editor

import androidx.compose.runtime.Composable
import formulaide.core.field.InputConstraints
import formulaide.ui.components.ChipContainer
import formulaide.ui.components.FilterChip

@Composable
fun SelectFieldType(field: MutableField, onReplace: (MutableField) -> Unit) = ChipContainer {
	FilterChip("Label", field is MutableField.Label, onUpdate = {
		if (it) {
			onReplace(MutableField.Label(field.label.value, field.source.value))
		}
	})
	FilterChip("Saisie", field is MutableField.Input, onUpdate = {
		if (it) {
			onReplace(MutableField.Input(field.label.value, InputConstraints.Text(), field.source.value))
		}
	})
	FilterChip("Choix", field is MutableField.Choice, onUpdate = {
		if (it) {
			onReplace(MutableField.Choice(field.label.value, emptyList(), field.source.value))
		}
	})
	FilterChip("Groupe", field is MutableField.Group, onUpdate = {
		if (it) {
			onReplace(MutableField.Group(field.label.value, emptyList(), field.source.value))
		}
	})
	FilterChip("Plusieurs réponses", field is MutableField.List, onUpdate = {
		if (it) {
			onReplace(MutableField.List(field.label.value, MutableField.Label("", null), 0u..2u, field.source.value))
		}
	})
}
