package formulaide.ui.components.text

import react.FC
import react.Props
import react.dom.html.ReactHTML.span

external interface TextProps : Props {
	var text: String

	var className: String?
}

private val TextProps.classNameOrEmpty get() = className ?: ""

/**
 * Grey text for information that is not too important.
 */
val LightText = FC<TextProps>("LightText") { props ->
	span {
		className = "text-gray-600 ${props.classNameOrEmpty}"
		+props.text
	}
}

/**
 * Red text to display errors to the user.
 */
val ErrorText = FC<TextProps>("ErrorText") { props ->
	span {
		className = "text-red-600 ${props.classNameOrEmpty}"
		+props.text
	}
}

/**
 * Smaller text for version information etc.
 */
val FooterText = FC<TextProps>("FooterText") { props ->
	span {
		className = "text-sm text-gray-600 mx-4 ${props.classNameOrEmpty}"
		+props.text
	}
}
