package formulaide.ui.components.text

import formulaide.ui.utils.classes
import react.FC
import react.Props
import react.dom.html.ReactHTML.span

external interface TextProps : Props {
	var text: String

	var classes: String?
}

private val TextProps.classesOrEmpty get() = this.classes ?: ""

/**
 * Grey text for information that is not too important.
 */
val LightText = FC<TextProps>("LightText") { props ->
	span {
		classes = "text-gray-600 ${props.classesOrEmpty}"
		+props.text
	}
}

/**
 * Red text to display errors to the user.
 */
val ErrorText = FC<TextProps>("ErrorText") { props ->
	span {
		classes = "text-red-600 ${props.classesOrEmpty}"
		+props.text
	}
}

/**
 * Smaller text for version information etc.
 */
val FooterText = FC<TextProps>("FooterText") { props ->
	span {
		classes = "text-sm text-gray-600 mx-4 ${props.classesOrEmpty}"
		+props.text
	}
}
