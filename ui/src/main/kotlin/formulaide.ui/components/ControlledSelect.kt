package formulaide.ui.components

import formulaide.ui.components.text.Text
import react.ChildrenBuilder
import react.FC
import react.Props
import react.dom.html.ReactHTML.option

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

private external interface ControlledSelectProps : Props {
	var options: List<ControlledSelectOption>
	var selected: ControlledSelectOption?
}

private val ControlledSelect = FC<ControlledSelectProps>("ControlledSelect") { props ->
	styledSelect(
		onSelect = { select ->
			props.options
				.first { it.value == select.value }
				.also { it.action() }
		}
	) {
		for (option in props.options) {
			option {
				Text { text = option.text }
				value = option.value
				selected = option == props.selected
			}
		}
	}
}

fun ChildrenBuilder.controlledSelect(
	builder: ControlledSelectOptionBuilder.() -> Unit,
) {
	val b = ControlledSelectOptionBuilder().apply(builder)

	ControlledSelect {
		options = b.options
		selected = b.selected ?: b.options.firstOrNull()
	}
}
