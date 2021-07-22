package formulaide.ui.components

import formulaide.ui.components2.useAsync
import formulaide.ui.reportExceptions
import formulaide.ui.utils.text
import kotlinx.html.HTMLTag
import kotlinx.html.InputType
import kotlinx.html.js.onClickFunction
import react.*
import react.dom.*

private const val buttonShapeClasses = "rounded-full py-1 px-3"
private const val buttonClasses = "$buttonShapeClasses hover:bg-purple-500"
private const val buttonDefaultClasses = "$buttonClasses bg-purple-800 text-white"
private const val buttonNonDefaultClasses =
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
			svg("animate-spin h-4 w-4") {
				attrs["viewBox"] = "0 0 24 24"

				tag({}, {
					HTMLTag(
						"circle",
						it,
						mapOf(
							"className" to "opacity-25",
							"cx" to "12",
							"cy" to "12",
							"r" to "10",
							"stroke" to "currentColor",
							"strokeWidth" to "4",
						),
						null,
						inlineTag = true,
						emptyTag = true)
				})

				tag({}, {
					HTMLTag(
						"path",
						it,
						mapOf(
							"className" to "opacity-100",
							"fill" to "currentColor",
							"d" to "M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z",
						),
						null,
						inlineTag = true,
						emptyTag = true)
				})
			}
		} else {
			text(props.text)
		}

		attrs {
			disabled = loading

			onClickFunction = {
				it.preventDefault()

				scope.reportExceptions {
					try {
						loading = true

						props.action()
					} finally {
						loading = false
					}
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
	span(buttonShapeClasses) {
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
