package formulaide.ui.components.cards

import formulaide.ui.components.*
import formulaide.ui.reportExceptions
import kotlinx.coroutines.CoroutineScope
import org.w3c.dom.HTMLFormElement
import react.FC
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.form
import react.dom.html.ReactHTML.span
import react.useState

external interface FormCardProps : CardProps {
	var submitAction: Pair<String, SubmitAction.(HTMLFormElement) -> Unit>
}

/**
 * Sets the [FormCardProps.submitAction].
 */
fun FormCardProps.submit(text: String, block: SubmitAction.(HTMLFormElement) -> Unit) {
	submitAction = (text to block)
}

val FormCard = FC<FormCardProps>("FormCard") { props ->
	val (submitText, submitAction) = props.submitAction
	val scope = useAsync()
	val actions = props.actions ?: emptyList()

	var loading by useState(props.loading ?: false)

	form {
		CardShell {
			this.id = props.id

			Header {
				CardTitle { +props }

				props.header?.invoke(this)
			}

			div {
				className = "py-4"
				props.children()
			}

			Footer {
				props.footer?.invoke(this)

				//region Submit button
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
				//endregion

				//region Other buttons
				for (action in actions) {
					StyledButton {
						text = action.first
						this.action = action.second
					}
				}
				//endregion
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

class SubmitAction(private val scope: CoroutineScope, private val setLoading: (Boolean) -> Unit) {
	fun launch(block: suspend () -> Unit) {
		scope.reportExceptions(onFailure = { setLoading(false) }) {
			setLoading(true)
			block()
			setLoading(false)
		}
	}
}
