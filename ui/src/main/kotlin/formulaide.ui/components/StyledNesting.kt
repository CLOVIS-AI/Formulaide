package formulaide.ui.components

import formulaide.ui.traceRenders
import kotlinx.html.DIV
import react.RBuilder
import react.dom.RDOMBuilder
import react.dom.div

private val colorPerDepth = listOf(
	listOf("bg-blue-100", "bg-blue-200"),
	listOf("bg-pink-100", "bg-pink-200"),
	listOf("bg-indigo-100", "bg-indigo-200"),
	listOf("bg-green-100", "bg-green-200"),
	listOf("bg-yellow-100", "bg-yellow-200"),
)

private const val spacing = "p-2 px-4 my-1"
private const val shape = "rounded-md"
private const val hover = "hover:shadow hover:mb-2"
private const val layout = "relative"
private const val nestingStyle = "$spacing $shape $hover $layout"

fun RBuilder.styledNesting(
	depth: Int? = null,
	fieldNumber: Int? = null,
	onDeletion: (suspend () -> Unit)? = null,
	onMoveUp: (suspend () -> Unit)? = null,
	onMoveDown: (suspend () -> Unit)? = null,
	block: RDOMBuilder<DIV>.() -> Unit,
) {
	traceRenders("styledNesting … depth $depth, number $fieldNumber")
	val selectedBackground = if (depth != null && fieldNumber != null) {
		val backgroundColors = colorPerDepth[depth % colorPerDepth.size]
		backgroundColors[fieldNumber % backgroundColors.size]
	} else ""

	div("$nestingStyle $selectedBackground") {
		block()

		div("m-2 absolute top-0 right-0") {
			if (onMoveUp != null)
				styledButton("▲", action = onMoveUp)

			if (onMoveDown != null)
				styledButton("▼", action = onMoveDown)

			if (onDeletion != null)
				styledButton("×", action = onDeletion)
		}
	}
}
