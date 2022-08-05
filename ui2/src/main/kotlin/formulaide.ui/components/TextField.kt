package formulaide.ui.components

import androidx.compose.runtime.*
import formulaide.ui.theme.Theme
import formulaide.ui.theme.shade
import formulaide.ui.utils.animateColor
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Label
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.TextInput

@Composable
fun TextField(
	label: String,
	value: String,
	onChange: (String) -> Unit,
) = Div {
	var focused by remember { mutableStateOf(false) }

	Label(
		null,
		{
			classes("text-field")
			style {
				paddingTop(2.px)
				paddingBottom(2.px)
				display(DisplayStyle.InlineBlock)
			}
		}
	) {
		Div {
			Text(label)
		}

		val borderColor =
			animateColor(if (focused) Theme.current.primary.background else Theme.current.neutral2.content)
		TextInput(value) {
			onInput { onChange(it.value) }

			style {
				shade(Theme.current.neutral2)
				outline("none")

				padding(4.px)
				if (!focused)
					marginBottom(1.px)

				property("border-bottom-width", if (focused) 2.px else 1.px)
				property("border-bottom-color", borderColor.css)
			}

			onFocusIn { focused = true }
			onFocusOut { focused = false }
		}
	}
}
