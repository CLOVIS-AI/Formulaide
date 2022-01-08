package formulaide.ui.components

import react.FC
import react.PropsWithChildren
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.span

val StyledPill = FC<PropsWithChildren> { props ->
	span {
		className = "$buttonShapeClasses bg-blue-200 " +
				"flex flex-shrink justify-between items-center gap-x-2 max-w-max pr-0 pl-0"
		props.children()
	}
}

val StyledPillContainer = FC<PropsWithChildren> { props ->
	div {
		className = "flex flex-row flex-wrap gap-2"
		props.children()
	}
}
