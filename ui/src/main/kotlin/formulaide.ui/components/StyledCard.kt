package formulaide.ui.components

import formulaide.ui.utils.text
import react.RBuilder
import react.dom.div
import react.dom.h2
import react.dom.p

fun RBuilder.styledCard(
	title: String,
	secondary: String?,
	vararg actions: Pair<String, () -> Unit>,
	contents: RBuilder.() -> Unit,
) {
	div("m-4 p-4 shadow-lg rounded-lg") {
		div("pb-4") {
			h2("text-xl") { text(title) }
			if (secondary != null) p("text-gray-600") { text(secondary) }
		}

		contents()

		for (action in actions) {
			styledButton(action.first) { action.second() }
		}
	}
}
