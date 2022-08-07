package formulaide.ui.components.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import formulaide.core.field.InputConstraints
import formulaide.ui.components.NumberField

@Composable
fun FieldMetadataEditor(
	field: MutableField,
) = when (field) {
	is MutableField.Choice -> {}
	is MutableField.Group -> {}
	is MutableField.Input -> {
		var input by field.input
		when (val inputCast = input) {
			InputConstraints.Boolean -> {}
			InputConstraints.Date -> {}
			InputConstraints.Email -> {}
			is InputConstraints.Integer -> {
				NumberField(
					"Valeur minimale autorisée",
					inputCast.effectiveMin,
					onChange = { input = inputCast.copy(min = it.toLong()) })
				NumberField(
					"Valeur maximale autorisée",
					inputCast.effectiveMax,
					onChange = { input = inputCast.copy(max = it.toLong()) })
			}

			InputConstraints.Phone -> {}
			is InputConstraints.Text -> {
				NumberField(
					"Nombre maximal de caractères",
					inputCast.effectiveMaxLength.toLong(),
					onChange = { input = inputCast.copy(maxLength = it.toLong().toUInt()) })
			}

			InputConstraints.Time -> {}
		}
	}

	is MutableField.Label -> {}
	is MutableField.List -> {
		var min by field.min
		var max by field.max

		NumberField("Nombre minimal de réponses", min.toInt(), onChange = { min = it.toInt().toUInt() })
		NumberField("Nombre maximal de réponses", max.toInt(), onChange = { max = it.toInt().toUInt() })
	}
}
