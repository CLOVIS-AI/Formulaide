package formulaide.ui.components.inputs

import react.FC
import react.PropsWithChildren
import react.dom.html.ReactHTML.option

external interface ControlledSelectProps : PropsWithChildren {
	var options: List<ControlledSelectOption>?
	var selected: ControlledSelectOption?
}

fun ControlledSelectProps.Option(text: String, value: String, selected: Boolean? = false, action: () -> Unit) {
	val option = ControlledSelectOption(text, value, action)

	options = (options ?: emptyList()) + option

	if (selected == true || this.selected == null)
		this.selected = option
}

data class ControlledSelectOption(
	val text: String,
	val value: String,
	val action: () -> Unit,
)

val ControlledSelect = FC<ControlledSelectProps>("ControlledSelect") { props ->
	Select {
		onSelection = { select ->
			props.options
				?.first { it.value == select.value }
				?.also { it.action() }
		}

		for (option in props.options ?: emptyList()) {
			option {
				+option.text
				value = option.value
				selected = option == props.selected
			}
		}
	}
}
