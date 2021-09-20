package formulaide.ui.components

import formulaide.ui.utils.text
import react.RBuilder
import react.dom.h2
import react.dom.span

fun RBuilder.styledTitle(text: String, loading: Boolean = false) {
	h2("text-xl") {
		text(text)
		if (loading) loadingSpinner()
	}
}

fun RBuilder.styledLightText(text: String) {
	span("text-gray-600") { text(text) }
}

fun RBuilder.styledErrorText(text: String) {
	span("text-red-600") { text(text) }
}

fun RBuilder.styledFooterText(text: String) {
	span("text-sm text-gray-600 mx-4") { text(text) }
}
