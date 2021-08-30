package formulaide.ui.components

import formulaide.ui.reportExceptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.html.DIV
import kotlinx.html.js.onSubmitFunction
import org.w3c.dom.HTMLFormElement
import react.*
import react.dom.*

private fun RBuilder.styledCardTitle(title: String, secondary: String?, loading: Boolean = false) {
	styledTitle(title, loading)
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

fun RBuilder.styledTitleCard(
	title: RBuilder.() -> Unit,
	actions: RBuilder.() -> Unit,
) {
	styledCardShell {
		div("flex flex-col-reverse justify-center md:flex-row md:justify-between md:items-center") {
			div {
				actions()
			}

			div("mb-2 md:mb-0") {
				title()
			}
		}
	}
}

private external interface FormCardProps : RProps {
	var title: String
	var secondary: String?
	var submit: Pair<String, SubmitAction.(HTMLFormElement) -> Unit>
	var actions: List<Pair<String, suspend () -> Unit>>
	var loading: Boolean
	var contents: (RBuilder) -> Unit
}

private val FormCard = fc<FormCardProps> { props ->
	val (submitText, submitAction) = props.submit
	val scope = useAsync()

	var loading by useState(props.loading)

	styledCardShell {
		form {
			styledCardTitle(props.title, props.secondary, loading)

			div("py-4") {
				props.contents(this)
			}

			if (!loading)
				styledSubmitButton(submitText, default = true)
			else
				span(classes = buttonNonDefaultClasses) { loadingSpinner() }

			for (action in props.actions) {
				styledButton(action.first, default = false) { action.second() }
			}

			attrs {
				onSubmitFunction = { event ->
					event.preventDefault()

					val submitActionDsl = SubmitAction(scope, setLoading = { loading = it })
					reportExceptions {
						submitActionDsl.submitAction(event.target as HTMLFormElement)
					}
				}
			}
		}
	}
}

fun RBuilder.styledFormCard(
	title: String,
	secondary: String?,
	submit: Pair<String, SubmitAction.(HTMLFormElement) -> Unit>,
	vararg actions: Pair<String, suspend () -> Unit>,
	loading: Boolean = false,
	contents: RBuilder.() -> Unit,
) {
	child(FormCard) {
		attrs {
			this.title = title
			this.secondary = secondary
			this.submit = submit
			this.actions = actions.asList()
			this.loading = loading
			this.contents = contents
		}
	}
}

class SubmitAction(private val scope: CoroutineScope, private val setLoading: (Boolean) -> Unit) {

	fun launch(block: suspend () -> Unit) {
		scope.reportExceptions(onFailure = { setLoading(false) }) {
			setLoading(true)
			block()
			setLoading(false)
		}
	}

}

fun RBuilder.styledFrame(block: RBuilder.() -> Unit) {
	div("xl:grid xl:grid-cols-5") {
		div {}
		div("xl:col-span-3") {
			block()
		}
		div {}
	}
}
