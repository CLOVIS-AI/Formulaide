package formulaide.ui.components

import formulaide.ui.utils.text
import kotlinx.html.InputType
import kotlinx.html.js.onClickFunction
import react.RBuilder
import react.dom.attrs
import react.dom.button
import react.dom.input

private const val buttonClasses = "rounded-full py-1 px-3 hover:bg-purple-500"
private const val buttonDefaultClasses = "bg-purple-800 text-white"
private const val buttonNonDefaultClasses = "text-purple-900 border-purple-800 hover:text-white"

fun RBuilder.styledButton(
	text: String,
	default: Boolean = false,
	action: () -> Unit,
) {
	button(classes = buttonClasses + " " + if (default) buttonDefaultClasses else buttonNonDefaultClasses) {
		text(text)

		attrs {
			onClickFunction = { action() }
		}
	}
}

fun RBuilder.styledSubmit(
	text: String,
	default: Boolean = true,
) {
	input(InputType.submit,
	      classes = buttonClasses + " " + if (default) buttonDefaultClasses else buttonNonDefaultClasses) {
		attrs {
			value = text
		}
	}
}
