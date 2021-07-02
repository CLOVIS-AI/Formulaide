package formulaide.ui.components

import formulaide.ui.utils.text
import kotlinx.html.INPUT
import kotlinx.html.InputType
import kotlinx.html.id
import kotlinx.html.js.onChangeFunction
import react.RBuilder
import react.dom.attrs
import react.dom.div
import react.dom.input
import react.dom.label

fun RBuilder.styledField(
	id: String,
	displayName: String,
	contents: RBuilder.() -> Unit,
) {
	styledFormField {
		label("block") {
			attrs["htmlFor"] = id

			text(displayName)
		}

		contents()
	}
}

fun RBuilder.styledInput(
	type: InputType,
	id: String,
	required: Boolean = false,
	handler: INPUT.() -> Unit = {},
) {
	input(type,
	      classes = "rounded bg-gray-200 border-b-2 border-gray-400 focus:border-purple-800 my-1 mr-3") {
		attrs {
			this.id = id
			this.name = id
			this.required = required

			handler()
		}
	}
	if (required)
		text(" *")
}

fun RBuilder.styledFormField(contents: RBuilder.() -> Unit) {
	div("mb-2") {
		contents()
	}
}

fun RBuilder.styledRadioButton(
	radioId: String,
	buttonId: String,
	value: String,
	text: String,
	checked: Boolean = false,
	onClick: () -> Unit = {},
) {
	input(InputType.radio, name = radioId, classes = "mr-1") {
		attrs {
			this.id = buttonId
			this.value = value
			this.checked = checked

			onChangeFunction = { onClick() }
		}
	}

	label(classes = "mr-2") {
		text(text)
		attrs["htmlFor"] = buttonId
	}
}
