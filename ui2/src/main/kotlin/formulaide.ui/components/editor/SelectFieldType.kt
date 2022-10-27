package formulaide.ui.components.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import formulaide.ui.components.ChipContainer
import formulaide.ui.components.FilterChip
import opensavvy.formulaide.core.InputConstraints

@Composable
fun SelectFieldType(field: MutableField, onReplace: (MutableField) -> Unit) {
	ChipContainer {
		FilterChip("Label", field is MutableField.Label, onUpdate = {
			if (it) {
				onReplace(MutableField.Label(field.label.value, field.importedFrom))
			}
		})
		FilterChip("Saisie", field is MutableField.Input, onUpdate = {
			if (it) {
				onReplace(MutableField.Input(field.label.value, InputConstraints.Text(), field.importedFrom))
			}
		})
		FilterChip("Choix", field is MutableField.Choice, onUpdate = {
			if (it) {
				onReplace(MutableField.Choice(field.label.value, emptyList(), field.importedFrom))
			}
		})
		FilterChip("Groupe", field is MutableField.Group, onUpdate = {
			if (it) {
				onReplace(MutableField.Group(field.label.value, emptyList(), field.importedFrom))
			}
		})
		FilterChip("Plusieurs réponses", field is MutableField.List, onUpdate = {
			if (it) {
				onReplace(
					MutableField.List(
						field.label.value,
						MutableField.Input("", InputConstraints.Text(), null),
						0u..2u,
						field.importedFrom
					)
				)
			}
		})
	}

	if (field is MutableField.Input) ChipContainer {
		var input by field.input
		FilterChip("Texte", input is InputConstraints.Text, onUpdate = {
			if (it) {
				input = InputConstraints.Text()
			}
		})
		FilterChip("Nombre", input is InputConstraints.Integer, onUpdate = {
			if (it) {
				input = InputConstraints.Integer()
			}
		})
		FilterChip("Coche", input is InputConstraints.Boolean, onUpdate = {
			if (it) {
				input = InputConstraints.Boolean
			}
		})
		FilterChip("Adresse mail", input is InputConstraints.Email, onUpdate = {
			if (it) {
				input = InputConstraints.Email
			}
		})
		FilterChip("Numéro de téléphone", input is InputConstraints.Phone, onUpdate = {
			if (it) {
				input = InputConstraints.Phone
			}
		})
		FilterChip("Date", input is InputConstraints.Date, onUpdate = {
			if (it) {
				input = InputConstraints.Date
			}
		})
		FilterChip("Heure", input is InputConstraints.Time, onUpdate = {
			if (it) {
				input = InputConstraints.Time
			}
		})
	}
}
