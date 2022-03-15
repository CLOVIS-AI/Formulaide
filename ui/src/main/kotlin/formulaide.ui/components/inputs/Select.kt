package formulaide.ui.components.inputs

import formulaide.ui.utils.classes
import org.w3c.dom.HTMLSelectElement
import react.FC
import react.PropsWithChildren
import react.dom.html.ReactHTML.select
import react.dom.html.SelectHTMLAttributes

external interface SelectProps : SelectHTMLAttributes<HTMLSelectElement>, PropsWithChildren {
	var onSelection: (HTMLSelectElement) -> Unit
}

val Select = FC<SelectProps>("Select") { props ->
	select {
		+props
		classes = largeInputStyle
		onChange = {
			props.onSelection(it.target)
		}
	}
}
