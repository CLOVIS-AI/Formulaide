package formulaide.ui.components

import formulaide.ui.utils.text
import react.RBuilder
import react.RProps
import react.child
import react.dom.attrs
import react.dom.option
import react.fc

data class ControlledSelectOption(
	val text: String,
	val value: String,
	val action: () -> Unit,
)

class ControlledSelectOptionBuilder {
	val options = ArrayList<ControlledSelectOption>()
	var selected: ControlledSelectOption? = null

	fun option(
		text: String,
		value: String,
		action: () -> Unit = {},
	) = ControlledSelectOption(text, value, action)
		.also { options.add(it) }

	fun ControlledSelectOption.select() {
		selected = this
	}

	fun ControlledSelectOption.selectIf(predicate: (ControlledSelectOption) -> Boolean) {
		if (predicate(this))
			this.select()
	}
}

private external interface ControlledSelectProps : RProps {
	var options: List<ControlledSelectOption>
	var selected: ControlledSelectOption?
}

private val ControlledSelect = fc<ControlledSelectProps> { props ->
	styledSelect(
		onSelect = { select ->
			props.options
				.first { it.value == select.value }
				.also { it.action() }
		}
	) {
		for (option in props.options) {
			option {
				text(option.text)
				attrs {
					value = option.value
					selected = option == props.selected
				}
			}
		}
	}
}

fun RBuilder.controlledSelect(
	builder: ControlledSelectOptionBuilder.() -> Unit,
) {
	val b = ControlledSelectOptionBuilder().apply(builder)

	child(ControlledSelect) {
		attrs {
			options = b.options
			selected = b.selected ?: b.options.firstOrNull()
		}
	}
}
