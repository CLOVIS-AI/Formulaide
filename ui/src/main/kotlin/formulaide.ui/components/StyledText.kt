package formulaide.ui.components

import formulaide.ui.utils.text
import react.ChildrenBuilder
import react.dom.html.ReactHTML.h2
import react.dom.html.ReactHTML.span

fun ChildrenBuilder.styledTitle(text: String, loading: Boolean = false) {
	h2 {
		className = "text-xl"

		text(text)
		if (loading) loadingSpinner()
	}
}

fun ChildrenBuilder.styledLightText(text: String) {
	span {
		className = "text-gray-600"
		text(text)
	}
}

fun ChildrenBuilder.styledErrorText(text: String) {
	span {
		className = "text-red-600"
		text(text)
	}
}

fun ChildrenBuilder.styledFooterText(text: String) {
	span {
		className = "text-sm text-gray-600 mx-4"
		text(text)
	}
}
