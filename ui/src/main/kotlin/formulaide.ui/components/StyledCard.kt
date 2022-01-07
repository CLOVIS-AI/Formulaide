package formulaide.ui.components

import formulaide.ui.components.cards.CardShell
import formulaide.ui.components.cards.CardTitle
import formulaide.ui.reportExceptions
import kotlinx.coroutines.CoroutineScope
import org.w3c.dom.HTMLFormElement
import react.*
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.form
import react.dom.html.ReactHTML.span

private external interface FormCardProps : Props {
	var title: String
	var secondary: String?
	var submit: Pair<String, SubmitAction.(HTMLFormElement) -> Unit>
	var actions: List<Pair<String, suspend () -> Unit>>
	var loading: Boolean
	var contents: (ChildrenBuilder) -> Unit
}

private val FormCard = FC<FormCardProps>("FormCard") { props ->
	val (submitText, submitAction) = props.submit
	val scope = useAsync()

	var loading by useState(props.loading)

	CardShell {
		form {
			CardTitle {
				this.title = props.title
				this.subtitle = props.secondary
				this.loading = loading
			}

			div {
				className = "py-4"
				props.contents(this)
			}

			if (!loading)
				StyledSubmitButton {
					text = submitText
					emphasize = true
				}
			else
				span {
					className = buttonNonDefaultClasses
					LoadingSpinner()
				}

			for (action in props.actions) {
				StyledButton {
					text = action.first
					this.action = action.second
				}
			}

			onSubmit = { event ->
				event.preventDefault()

				val submitActionDsl = SubmitAction(scope, setLoading = { loading = it })
				reportExceptions {
					submitActionDsl.submitAction(event.target as HTMLFormElement)
				}
			}
		}
	}
}

fun ChildrenBuilder.styledFormCard(
	title: String,
	secondary: String?,
	submit: Pair<String, SubmitAction.(HTMLFormElement) -> Unit>,
	vararg actions: Pair<String, suspend () -> Unit>,
	loading: Boolean = false,
	contents: ChildrenBuilder.() -> Unit,
) {
	FormCard {
		this.title = title
		this.secondary = secondary
		this.submit = submit
		this.actions = actions.asList()
		this.loading = loading
		this.contents = contents
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

val StyledFrame = FC<PropsWithChildren> { props ->
	div {
		className = "lg:grid lg:grid-cols-9 xl:grid-cols-7"

		div {}
		div {
			className = "lg:col-span-7 xl:col-span-5"
			props.children()
		}
		div {}
	}
}
