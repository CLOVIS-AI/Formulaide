package formulaide.ui.components

import react.FC
import react.PropsWithChildren
import react.dom.html.ReactHTML.div

val StyledFrame = FC<PropsWithChildren> { props ->
	div {
		className = "lg:grid lg:grid-cols-9 xl:grid-cols-7"

		div {}
		div {
			className = "lg:col-span-7 xl:col-span-5"
			props.children()
		}
		div {}
	}
}
