package formulaide.ui.components.inputs

import react.FC
import react.PropsWithChildren
import react.dom.html.ReactHTML.div

/**
 * Correct spacing etc to include a field of a form.
 */
val FormField = FC<PropsWithChildren>("FormField") { props ->
	div {
		className = "mb-2"
		props.children()
	}
}
