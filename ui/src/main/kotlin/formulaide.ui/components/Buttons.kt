package formulaide.ui.components

import formulaide.ui.reportExceptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import react.FC
import react.Props
import react.dom.html.InputType
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.span
import react.useState

internal const val buttonShapeClasses = "rounded-full py-1 px-3 mx-1"
internal const val buttonClasses = "$buttonShapeClasses hover:bg-purple-500"
internal const val buttonDefaultClasses = "$buttonClasses bg-purple-800 text-white"
internal const val buttonNonDefaultClasses =
	"$buttonClasses text-purple-800 border-1 border-purple-800 hover:text-white"

external interface StyledButtonProps : Props {
	/**
	 * The text displayed by the button.
	 */
	var text: String?

	/**
	 * `true` if this button is the emphasized option.
	 */
	var emphasize: Boolean?

	/**
	 * `true` if this button can be interacted with.
	 */
	var enabled: Boolean?

	/**
	 * The action executed when the button is pressed.
	 */
	var action: (suspend () -> Unit)?
}

val StyledButton = FC<StyledButtonProps>("StyledButton") { props ->
	val scope = useAsync()

	var loading by useState(false)

	if (props.enabled != false) {
		button {
			className = if (props.emphasize == true) buttonDefaultClasses else buttonNonDefaultClasses
			disabled = loading

			if (loading)
				LoadingSpinner()
			else
				+(props.text ?: "")

			onClick = {
				it.preventDefault()

				val startLoading = scope.launch {
					delay(10)
					loading = true
				}

				scope.reportExceptions(onFailure = { loading = false }) {
					props.action?.invoke()
					startLoading.cancel()

					loading = false
				}
			}
		}
	} else {
		span {
			+(props.text ?: "")
			className = "$buttonShapeClasses font-bold"
		}
	}
}

val StyledSubmitButton = FC<StyledButtonProps>("StyledSubmitButton") { props ->
	if (props.action != null)
		console.warn("A StyledSubmitButton was provided an 'action', but it is ignored, as the form's action will be executed instead.",
		             props)

	input {
		type = InputType.submit
		className = if (props.emphasize == true) buttonDefaultClasses else buttonNonDefaultClasses
		value = props.text
		disabled = !(props.enabled ?: true)
	}
}
