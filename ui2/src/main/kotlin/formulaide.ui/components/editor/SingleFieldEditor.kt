package formulaide.ui.components.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import formulaide.ui.components.TextButton
import formulaide.ui.components.TextField
import formulaide.ui.theme.Theme
import formulaide.ui.theme.shade
import opensavvy.formulaide.core.Field
import opensavvy.formulaide.core.InputConstraints
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

@Composable
fun SingleFieldEditor(
	id: Field.Id,
	field: MutableField,
	onReplace: (Field.Id, MutableField) -> Unit,
	onSelect: (Field.Id) -> Unit,
) {
	Div(
		{
			style {
				color(Theme.current.secondaryContainer.content.css)
				backgroundColor(Theme.current.secondaryContainer.background.css)
				padding(10.px)
				borderRadius(10.px)

				display(DisplayStyle.Flex)
				flexDirection(FlexDirection.Column)
				alignItems(AlignItems.Start)
				gap(5.px)
			}
		}
	) {
		var label by field.label

		TextField("Libellé", label, onChange = { label = it })

		if (field.importedFrom == null) {
			SelectFieldType(field, onReplace = { onReplace(id, it) })
			FieldMetadataEditor(field)
		}

		ChildChooser(field, onSelect = { onSelect(Field.Id(id.parts + it)) })

		if (field.importedFrom == null && (field is MutableField.Group || field is MutableField.Choice))
			TextButton(onClick = {
				val newId = field.fields.keys.maxOrNull()?.plus(1) ?: 0
				(field.fields as MutableMap)[newId] = MutableField.Input("", InputConstraints.Text(), null)
				onSelect(id + newId)
			}) {
				Text(if (field is MutableField.Group) "Nouveau champ" else "Nouvelle option")
			}
	}
}

@Composable
private fun ChildChooser(child: MutableField, onSelect: (List<Int>) -> Unit, isRoot: Boolean = true) {
	val label by child.label

	if (!isRoot) {
		Div {
			Button(
				{
					onClick { onSelect(emptyList()) }
				}
			) {
				val error = run {
					try {
						child.fields.mapValues { (_, v) -> v.toField() }
					} catch (e: Exception) {
						return@run null // one of the children is invalid, ignore the parent to avoid mentioning the problem multiple times
					}

					try {
						child.toField()
						null
					} catch (e: Exception) {
						e
					}
				}

				if (error != null) Span(
					{
						style {
							shade(Theme.current.error)
						}
					}
				) {
					Text(error.message ?: error.toString())
				} else {
					Text(label)
				}

				Text(" \uD83D\uDD89")

				Span(
					{
						style {
							color(Theme.current.primary.content.copy(alpha = 0.5).css)
							marginLeft(0.5.em)
						}
					}
				) {
					Text(
						when (child) {
							is MutableField.Choice -> "Choix"
							is MutableField.Group -> "Groupe"
							is MutableField.Input -> when (child.input.value) {
								InputConstraints.Boolean -> "Case à cocher"
								InputConstraints.Date -> "Date"
								InputConstraints.Email -> "Adresse mail"
								is InputConstraints.Integer -> "Nombre"
								InputConstraints.Phone -> "Numéro de téléphone"
								is InputConstraints.Text -> "Texte"
								InputConstraints.Time -> "Heure"
							}

							is MutableField.Label -> "Label"
							is MutableField.List -> "De ${child.min.value} à ${child.max.value} réponses"
						}
					)
				}
			}
		}
	}

	val subfields = when (child) {
		is MutableField.List -> mapOf(0 to child.field.value)
		is MutableField.Group -> child.fields
		is MutableField.Choice -> child.fields
		else -> emptyMap()
	}

	for ((subfieldId, subfield) in subfields) {
		Div(
			{
				style {
					if (!isRoot) {
						marginLeft(20.px)
						marginRight(20.px)
					}
				}
			}
		) {
			ChildChooser(subfield, onSelect = { onSelect(listOf(subfieldId) + it) }, isRoot = false)
		}
	}
}
