package formulaide.ui.components

import formulaide.ui.traceRenders
import org.w3c.dom.HTMLDivElement
import react.ChildrenBuilder
import react.dom.html.HTMLAttributes
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
private const val hover = "hover:shadow hover:mb-2"
private const val layout = "relative"
private const val nestingStyle = "$spacing $shape $hover $layout"

fun ChildrenBuilder.styledNesting(
	depth: Int? = null,
	fieldNumber: Int? = null,
	onDeletion: (suspend () -> Unit)? = null,
	onMoveUp: (suspend () -> Unit)? = null,
	onMoveDown: (suspend () -> Unit)? = null,
	block: HTMLAttributes<HTMLDivElement>.() -> Unit,
) {
	traceRenders("styledNesting … depth $depth, number $fieldNumber")
	val selectedBackground = if (depth != null && fieldNumber != null) {
		val backgroundColors = colorPerDepth[depth % colorPerDepth.size]
		backgroundColors[fieldNumber % backgroundColors.size]
	} else ""

	div {
		className = "$nestingStyle $selectedBackground"

		block()

		div {
			className = "m-2 absolute top-0 right-0"

			if (onMoveUp != null)
				StyledButton {
					text = "▲"
					action = onMoveUp
				}

			if (onMoveDown != null)
				StyledButton {
					text = "▼"
					action = onMoveDown
				}

			if (onDeletion != null)
				StyledButton {
					text = "×"
					action = onDeletion
				}
		}
	}
}
