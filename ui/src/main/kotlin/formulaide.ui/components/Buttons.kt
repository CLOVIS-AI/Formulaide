package formulaide.ui.components

import formulaide.ui.reportExceptions
import formulaide.ui.utils.text
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.html.DIV
import kotlinx.html.InputType
import kotlinx.html.SPAN
import kotlinx.html.js.onClickFunction
import react.*
import react.dom.*

private const val buttonShapeClasses = "rounded-full py-1 px-3 mx-1"
private const val buttonClasses = "$buttonShapeClasses hover:bg-purple-500"
internal const val buttonDefaultClasses = "$buttonClasses bg-purple-800 text-white"
internal const val buttonNonDefaultClasses =
	"$buttonClasses text-purple-800 border-1 border-purple-800 hover:text-white"

internal external interface ButtonProps : RProps {
	var text: String
	var default: Boolean

	var action: suspend () -> Unit
}

private val CustomButton = fc<ButtonProps> { props ->
	val scope = useAsync()

	var loading by useState(false)

	val classes =
		if (props.default) buttonDefaultClasses
		else buttonNonDefaultClasses

	button(classes = classes) {
		if (loading) {
			loadingSpinner()
		} else {
			text(props.text)
		}

		attrs {
			disabled = loading

			onClickFunction = {
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
}

fun RBuilder.styledButton(
	text: String,
	default: Boolean = false,
	action: suspend () -> Unit,
) {
	child(CustomButton) {
		attrs {
			this.text = text
			this.default = default
			this.action = action
		}
	}
}

fun RBuilder.styledDisabledButton(
	text: String,
) {
	span("$buttonShapeClasses font-bold") {
		text(text)
	}
}

fun RBuilder.styledSubmitButton(
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

fun RBuilder.styledPill(
	contents: RDOMBuilder<SPAN>.() -> Unit,
) {
	span("$buttonShapeClasses bg-blue-200 flex flex-shrink justify-between items-center gap-x-2 max-w-max pr-0 pl-0") {
		contents()
	}
}

fun RBuilder.styledPillContainer(
	contents: RDOMBuilder<DIV>.() -> Unit,
) {
	div("flex flex-row flex-wrap gap-2") {
		contents()
	}
}
