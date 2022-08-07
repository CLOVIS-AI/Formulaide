package formulaide.ui.components

import androidx.compose.runtime.*
import formulaide.ui.theme.Theme
import formulaide.ui.theme.shade
import formulaide.ui.utils.animateColor
import org.jetbrains.compose.web.attributes.builders.InputAttrsScope
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*

@Composable
private fun AbstractField(
	label: String,
	value: String,
	onChange: (String) -> Unit,
	input: @Composable (value: String, onChange: (String) -> Unit, attrs: InputAttrsScope<*>.() -> Unit) -> Unit,
) = Div {
	var focused by remember { mutableStateOf(false) }

	Label(
		null,
		{
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

		input(value, onChange) {
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

@Composable
fun TextField(
	label: String,
	value: String,
	onChange: (String) -> Unit,
) = AbstractField(label, value, onChange) { innerValue, innerOnChange, attrs ->
	TextInput(innerValue) {
		onInput { innerOnChange(it.value) }
		attrs()
	}
}

@Composable
fun PasswordField(
	label: String,
	value: String,
	onChange: (String) -> Unit,
) = AbstractField(label, value, onChange) { innerValue, innerOnChange, attrs ->
	PasswordInput(innerValue) {
		onInput { innerOnChange(it.value) }
		attrs()
	}
}

@Composable
fun NumberField(
	label: String,
	value: Number,
	onChange: (Number) -> Unit,
) = AbstractField(label, value.toString(), onChange = { onChange(it.toDouble()) }) { innerValue, innerOnChange, attrs ->
	NumberInput(innerValue.toDouble()) {
		onInput { innerOnChange(it.value.toString()) }
		attrs()
	}
}
