package formulaide.ui.components

import kotlinx.html.DIV
import kotlinx.html.FORM
import react.RBuilder
import react.dom.*

private fun RBuilder.styledCardTitle(title: String, secondary: String?, loading: Boolean = false) {
	styledTitle(title)
	if (loading) loadingSpinner()
	if (secondary != null) p { styledLightText(secondary) }
}

private fun RBuilder.styledCardShell(
	failed: Boolean = false,
	contents: RDOMBuilder<DIV>.() -> Unit,
) {
	var classes = "m-4 p-4 shadow-2xl rounded-lg z-10 relative bg-white"

	if (failed)
		classes += " bg-red-200"

	div(classes) {
		contents()
	}
}

fun RBuilder.styledCard(
	title: String,
	secondary: String? = null,
	vararg actions: Pair<String, suspend () -> Unit>,
	failed: Boolean = false,
	loading: Boolean = false,
	contents: RBuilder.() -> Unit,
) {
	styledCardShell(failed) {
		styledCardTitle(title, secondary, loading)

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
	loading: Boolean = false,
	contents: RBuilder.() -> Unit,
	handler: FORM.() -> Unit,
) {
	styledCardShell {
		form {
			styledCardTitle(title, secondary, loading)

			div("py-4") {
				contents()
			}

			styledSubmitButton(submit, default = true)
			for (action in actions) {
				styledButton(action.first, default = false) { action.second() }
			}

			attrs(handler)
		}
	}
}
