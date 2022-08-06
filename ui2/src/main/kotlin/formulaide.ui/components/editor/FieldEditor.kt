package formulaide.ui.components.editor

import androidx.compose.runtime.*
import formulaide.ui.components.DisplayError
import formulaide.ui.components.TextField
import formulaide.ui.theme.Theme
import formulaide.ui.theme.shade
import formulaide.ui.utils.animateShade
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

@Composable
private fun checkFieldValidity(field: MutableField) = remember(field) {
	derivedStateOf {
		try {
			field.fields.forEach { it.value.toField() }
		} catch (e: Exception) {
			// If an error happens while checking a subfield, ignore it
			return@derivedStateOf null
		}

		try {
			field.toField()
			null
		} catch (e: Throwable) {
			e
		}
	}
}

@Composable
fun FieldEditor(
	field: MutableField,
	onReplace: (MutableField) -> Unit,
) {
	var focused by remember { mutableStateOf(true) }
	var label by field.label
	val source by field.source

	val shade = animateShade(
		when {
			focused -> Theme.current.neutral1
			else -> Theme.current.default
		}
	)

	val error by checkFieldValidity(field)

	Div(
		{
			tabIndex(0)
			onFocusIn { focused = true }
			onFocusOut { focused = false }

			style {
				shade(shade)
				marginTop(5.px)

				if (focused) {
					borderRadius(8.px)
					border {
						width = 1.px
						style = LineStyle.Solid
						color = Theme.current.neutral2.content.css
					}
					padding(5.px)
				}

				paddingLeft(10.px)
				paddingRight(10.px)
			}
		}
	) {
		if (focused) {
			TextField("Label", label, onChange = { label = it })
			if (error != null)
				DisplayError(error!!)

			if (source == null)
				SelectFieldType(field, onReplace)
		} else if (error != null) {
			DisplayError(error!!)
		} else {
			Span(
				{
					style {
						color(Theme.current.neutral1.content.css)
					}
				}
			) {
				Text(label)
			}
			Text(" : ")
			when (field) {
				is MutableField.Choice -> Text("choix")
				is MutableField.Group -> Text("groupe")
				is MutableField.Input -> Text("saisie")
				is MutableField.Label -> Text("label")
				is MutableField.List -> Text("de ${field.min.value} à ${field.max.value} réponses")
			}
		}

		SubfieldEditor(field, focused)
	}
}
