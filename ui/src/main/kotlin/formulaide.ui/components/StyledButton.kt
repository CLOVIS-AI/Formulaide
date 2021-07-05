package formulaide.ui.components

import formulaide.ui.utils.text
import kotlinx.html.InputType
import kotlinx.html.js.onClickFunction
import react.RBuilder
import react.dom.attrs
import react.dom.button
import react.dom.input
import react.dom.span

private const val buttonShapeClasses = "rounded-full py-1 px-3"
private const val buttonClasses = "$buttonShapeClasses hover:bg-purple-500"
private const val buttonDefaultClasses = "$buttonClasses bg-purple-800 text-white"
private const val buttonNonDefaultClasses =
	"$buttonClasses text-purple-800 border-1 border-purple-800 hover:text-white"

fun RBuilder.styledButton(
	text: String,
	default: Boolean = false,
	action: () -> Unit,
) {
	button(classes = if (default) buttonDefaultClasses else buttonNonDefaultClasses) {
		text(text)

		attrs {
			onClickFunction = { it.preventDefault(); action() }
		}
	}
}

fun RBuilder.styledDisabledButton(
	text: String,
) {
	span(buttonShapeClasses) {
		text(text)
	}
}

fun RBuilder.styledSubmit(
	text: String,
	default: Boolean = true,
) {
	input(InputType.submit,
	      classes = if (default) buttonDefaultClasses else buttonNonDefaultClasses) {
		attrs {
			value = text
		}
	}
}
