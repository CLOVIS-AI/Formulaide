package formulaide.ui.components.inputs

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
		className = largeInputStyle
		props.onSelect = { props.onSelection(it.target as HTMLSelectElement) }
	}
}
