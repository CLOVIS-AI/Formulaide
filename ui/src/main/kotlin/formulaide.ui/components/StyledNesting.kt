package formulaide.ui.components

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

fun RBuilder.styledNesting(
	depth: Int? = null,
	fieldNumber: Int? = null,
	block: RDOMBuilder<DIV>.() -> Unit,
) {
	val selectedBackground = if (depth != null && fieldNumber != null) {
		val backgroundColors = colorPerDepth[depth % colorPerDepth.size]
		backgroundColors[fieldNumber % backgroundColors.size]
	} else ""

	div("p-2 px-4 my-1 rounded-md hover:shadow hover:mb-2 $selectedBackground") {
		block()
	}
}
