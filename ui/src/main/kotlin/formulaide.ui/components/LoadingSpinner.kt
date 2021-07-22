package formulaide.ui.components

import kotlinx.html.HTMLTag
import react.RBuilder
import react.dom.svg
import react.dom.tag

fun RBuilder.loadingSpinner() {
	svg("animate-spin h-4 w-4") {
		attrs["viewBox"] = "0 0 24 24"

		tag({}, {
			HTMLTag(
				"circle",
				it,
				mapOf(
					"className" to "opacity-25",
					"cx" to "12",
					"cy" to "12",
					"r" to "10",
					"stroke" to "currentColor",
					"strokeWidth" to "4",
				),
				null,
				inlineTag = true,
				emptyTag = true)
		})

		tag({}, {
			HTMLTag(
				"path",
				it,
				mapOf(
					"className" to "opacity-100",
					"fill" to "currentColor",
					"d" to "M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z",
				),
				null,
				inlineTag = true,
				emptyTag = true)
		})
	}
}
