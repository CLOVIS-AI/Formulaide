package formulaide.ui.components.inputs

import formulaide.ui.utils.classes
import react.FC
import react.PropsWithChildren
import react.dom.html.ReactHTML.div

/**
 * Correct spacing etc to include a field of a form.
 */
val FormField = FC<PropsWithChildren>("FormField") { props ->
	div {
		classes = "mb-2"
		+props.children
	}
}
