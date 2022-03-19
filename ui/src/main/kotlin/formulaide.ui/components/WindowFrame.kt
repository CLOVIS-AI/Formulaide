package formulaide.ui.components

import formulaide.ui.utils.classes
import react.FC
import react.PropsWithChildren
import react.dom.html.ReactHTML.div

val WindowFrame = FC<PropsWithChildren> { props ->
	div {
		classes = "lg:grid lg:grid-cols-9 xl:grid-cols-7"

		div {}
		div {
			classes = "lg:col-span-7 xl:col-span-5"
			+props.children
		}
		div {}
	}
}
