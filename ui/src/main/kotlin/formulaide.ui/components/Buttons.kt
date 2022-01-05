package formulaide.ui.components

import formulaide.ui.reportExceptions
import formulaide.ui.utils.text
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import react.ChildrenBuilder
import react.FC
import react.Props
import react.dom.html.InputType
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.span
import react.useState

private const val buttonShapeClasses = "rounded-full py-1 px-3 mx-1"
private const val buttonClasses = "$buttonShapeClasses hover:bg-purple-500"
internal const val buttonDefaultClasses = "$buttonClasses bg-purple-800 text-white"
internal const val buttonNonDefaultClasses =
	"$buttonClasses text-purple-800 border-1 border-purple-800 hover:text-white"

internal external interface ButtonProps : Props {
	var text: String
	var default: Boolean

	var action: suspend () -> Unit
}

private val CustomButton = FC<ButtonProps>("CustomButton") { props ->
	val scope = useAsync()

	var loading by useState(false)

	val classes = if (props.default) buttonDefaultClasses
	else buttonNonDefaultClasses

	button {
		if (loading) {
			loadingSpinner()
		} else {
			text(props.text)
		}

		className = classes
		disabled = loading

		onClick = {
			it.preventDefault()

			val startLoading = scope.launch {
				delay(10)
				loading = true
			}

			scope.reportExceptions(onFailure = { loading = false }) {
				props.action()
				startLoading.cancel()

				loading = false
			}
		}
	}
}

fun ChildrenBuilder.styledButton(
	text: String,
	default: Boolean = false,
	enabled: Boolean = true,
	action: suspend () -> Unit,
) {
	if (enabled) {
		CustomButton {
			this.text = text
			this.default = default
			this.action = action
		}
	} else
		styledDisabledButton(text)
}

fun ChildrenBuilder.styledDisabledButton(
	text: String,
) {
	span {
		text(text)
		className = "$buttonShapeClasses font-bold"
	}
}

fun ChildrenBuilder.styledSubmitButton(
	text: String,
	default: Boolean = true,
) {
	input {
		type = InputType.submit
		className = if (default) buttonDefaultClasses else buttonNonDefaultClasses
		value = text
	}
}

fun ChildrenBuilder.styledPill(
	contents: ChildrenBuilder.() -> Unit,
) {
	span {
		className = "$buttonShapeClasses bg-blue-200 flex flex-shrink justify-between items-center gap-x-2 max-w-max pr-0 pl-0"
		contents()
	}
}

fun ChildrenBuilder.styledPillContainer(
	contents: ChildrenBuilder.() -> Unit,
) {
	div {
		className = "flex flex-row flex-wrap gap-2"
		contents()
	}
}
