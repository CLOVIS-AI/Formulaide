package formulaide.ui.components.inputs

import formulaide.ui.components.StyledButton
import formulaide.ui.utils.classes
import react.FC
import react.PropsWithChildren
import react.dom.html.ReactHTML.div

private val colorPerDepth = listOf(
	listOf("bg-blue-100", "bg-blue-200"),
	listOf("bg-pink-100", "bg-pink-200"),
	listOf("bg-indigo-100", "bg-indigo-200"),
	listOf("bg-green-100", "bg-green-200"),
	listOf("bg-yellow-100", "bg-yellow-200"),
)

private const val spacing = "p-2 px-4 my-1"
private const val shape = "rounded-md"
private const val print = "print:border-solid print:border-2 print:bg-inherit"
private const val hover = "hover:shadow"
private const val layout = "relative"
private const val nestingStyle = "$spacing $shape $hover $layout $print"

external interface NestingProps : PropsWithChildren {
	var depth: Int?
	var fieldNumber: Int?

	var onDeletion: (suspend () -> Unit)?
	var onMoveUp: (suspend () -> Unit)?
	var onMoveDown: (suspend () -> Unit)?
}

val Nesting = FC<NestingProps>("Nesting") { props ->
	val depth = props.depth ?: 0
	val fieldNumber = props.fieldNumber ?: 0

	val selectedBackground = if (props.depth != null && props.fieldNumber != null) {
		val backgroundColors = colorPerDepth[depth % colorPerDepth.size]
		backgroundColors[fieldNumber % backgroundColors.size]
	} else ""

	div {
		classes = "$nestingStyle $selectedBackground"

		+props.children

		div {
			classes = "m-2 absolute top-0 right-0"

			if (props.onMoveUp != null)
				StyledButton {
					text = "▲"
					action = props.onMoveUp
				}

			if (props.onMoveDown != null)
				StyledButton {
					text = "▼"
					action = props.onMoveDown
				}

			if (props.onDeletion != null)
				StyledButton {
					text = "×"
					action = props.onDeletion
				}
		}
	}
}
