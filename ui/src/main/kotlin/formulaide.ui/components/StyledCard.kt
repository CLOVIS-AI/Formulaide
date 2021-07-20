package formulaide.ui.components

import kotlinx.html.DIV
import kotlinx.html.FORM
import react.RBuilder
import react.dom.*

private fun RBuilder.styledCardTitle(title: String, secondary: String?) {
	styledTitle(title)
	if (secondary != null) p { styledLightText(secondary) }
}

private fun RBuilder.styledCardShell(
	failed: Boolean = false,
	contents: RDOMBuilder<DIV>.() -> Unit,
) {
	var classes = "m-4 p-4 shadow-xl rounded-lg"

	if (failed)
		classes += " bg-red-200"

	div(classes) {
		contents()
	}
}

fun RBuilder.styledCard(
	title: String,
	secondary: String? = null,
	vararg actions: Pair<String, () -> Unit>,
	failed: Boolean = false,
	contents: RBuilder.() -> Unit,
) {
	styledCardShell(failed) {
		styledCardTitle(title, secondary)

		div("pt-4") {
			contents()
		}

		if (actions.isNotEmpty()) div("pt-4") {
			for (action in actions) {
				styledButton(action.first,
				             default = action == actions.first()) { action.second() }
			}
		}
	}
}

fun RBuilder.styledFormCard(
	title: String,
	secondary: String?,
	submit: String,
	vararg actions: Pair<String, () -> Unit>,
	contents: RBuilder.() -> Unit,
	handler: FORM.() -> Unit,
) {
	styledCardShell {
		form {
			styledCardTitle(title, secondary)

			div("py-4") {
				contents()
			}

			styledSubmit(submit, default = true)
			for (action in actions) {
				styledButton(action.first, default = false) { action.second() }
			}

			attrs(handler)
		}
	}
}
