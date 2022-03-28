package formulaide.ui.components.cards

import formulaide.ui.utils.classes
import react.ChildrenBuilder
import react.FC
import react.Props
import react.PropsWithChildren
import react.dom.html.ReactHTML.div

external interface CommonCardProps : Props {
	var id: String?
	var failed: Boolean?
	var mini: Boolean?

	var header: ((ChildrenBuilder) -> Unit)?
	var footer: ((ChildrenBuilder) -> Unit)?
}

external interface CardShellProps : PropsWithChildren, CommonCardProps

val CardShell = FC<CardShellProps>("CardShell") { props ->
	val paddingTop = if (props.mini == true) "pt-4" else "pt-8"
	val paddingBottom = if (props.mini == true) "pb-4" else "pb-8"
	val color = if (props.failed == true) "bg-red-200" else ""

	div {
		this.id = props.id
		classes = "m-4 px-8 shadow-lg rounded-lg z-10 relative bg-white $color"

		props.header?.let { header ->
			div {
				classes = "sticky top-0 z-40 bg-inherit $paddingTop pb-2"

				header(this)
			}
		}

		+props.children

		props.footer?.let { footer ->
			div {
				classes = "sticky bottom-0 z-40 bg-inherit $paddingBottom pt-2"

				footer(this)
			}
		}
	}
}

@Suppress("FunctionName") // This function looks like a component
fun CommonCardProps.Header(block: ChildrenBuilder.() -> Unit) {
	header = block
}

@Suppress("FunctionName") // This function looks like a component
fun CommonCardProps.Footer(block: ChildrenBuilder.() -> Unit) {
	footer = block
}
