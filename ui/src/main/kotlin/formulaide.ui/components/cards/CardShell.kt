package formulaide.ui.components.cards

import react.FC
import react.PropsWithChildren
import react.dom.html.ReactHTML.div

external interface CardShellProps : PropsWithChildren {
	var id: String?
	var failed: Boolean?
	var mini: Boolean?
}

val CardShell = FC<CardShellProps>("CardShell") { props ->
	div {
		this.id = props.id
		className = "m-4 px-8 shadow-lg rounded-lg z-10 relative bg-white" +
				(if (props.mini == true) " py-4" else " py-8") +
				(if (props.failed == true) " bg-red-200" else "")
		props.children()
	}
}
