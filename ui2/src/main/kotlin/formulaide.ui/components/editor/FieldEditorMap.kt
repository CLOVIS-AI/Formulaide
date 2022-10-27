package formulaide.ui.components.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import formulaide.ui.theme.CustomColor
import formulaide.ui.theme.Shade
import formulaide.ui.theme.Theme
import formulaide.ui.theme.shade
import opensavvy.formulaide.core.Field
import opensavvy.formulaide.core.InputConstraints
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

@Composable
private fun MapButton(
	text: String,
	isSelected: Boolean,
	onSelect: () -> Unit,
) {
	Button(
		{
			onClick { onSelect() }

			style {
				marginRight(5.px)
			}

			if (isSelected)
				disabled()
			else style {
				shade(Shade(Theme.current.primary.background, CustomColor.transparent))
			}
		}
	) {
		Text(text)
	}
}

@Composable
private fun MapButton(
	field: MutableField,
	isSelected: Boolean,
	onSelect: () -> Unit,
) {
	val label by field.label

	MapButton(label.ifBlank { "Libellé manquant" }, isSelected, onSelect)
}

@Composable
private fun ChildTypeIndicator(
	field: MutableField,
) = Span(
	{
		style {
			color(Theme.current.primary.content.copy(alpha = 0.5).css)
		}
	}
) {
	Text(
		when (field) {
			is MutableField.Choice -> "Choix"
			is MutableField.Group -> "Groupe"
			is MutableField.Input -> when (field.input.value) {
				InputConstraints.Boolean -> "Case à cocher"
				InputConstraints.Date -> "Date"
				InputConstraints.Email -> "Adresse mail"
				is InputConstraints.Integer -> "Nombre"
				InputConstraints.Phone -> "Numéro de téléphone"
				is InputConstraints.Text -> "Texte"
				InputConstraints.Time -> "Heure"
			}

			is MutableField.Label -> "Label"
			is MutableField.List -> "De ${field.min.value} à ${field.max.value} réponses"
		}
	)
}

@Composable
private fun FieldSelectorChild(
	root: MutableField,
	current: MutableField,
	currentId: Field.Id,
	selectedId: Field.Id,
	onSelect: (Field.Id) -> Unit,
): Unit = Div(
	{
		style {
			if (root != current) {
				marginLeft(20.px)
				marginRight(20.px)
			}
		}
	}
) {
	MapButton(current, isSelected = currentId == selectedId, onSelect = { onSelect(currentId) })

	ChildTypeIndicator(current)

	for ((id, child) in current.fields) {
		FieldSelectorChild(root, child, currentId + id, selectedId, onSelect)
	}

	if (current.importedFrom == null && (current is MutableField.Group || current is MutableField.Choice)) {
		Div(
			{
				style {
					marginLeft(20.px)
					marginRight(20.px)
				}
			}
		) {
			val childType = if (current is MutableField.Group) "champ" else "option"

			MapButton("+ $childType", isSelected = false, onSelect = {
				val newId = current.fields.keys.maxOrNull()?.plus(1) ?: 0
				(current.fields as MutableMap)[newId] = MutableField.Input("", InputConstraints.Text(), null)
				onSelect(currentId + newId)
			})
		}
	}
}

@Composable
fun FieldSelector(
	root: MutableField,
	selected: Field.Id,
	onSelect: (Field.Id) -> Unit,
) = Div(
	{
		style {
			marginBottom(5.px)
		}
	}
) {
	FieldSelectorChild(root, root, Field.Id.root, selected, onSelect)
}
