package formulaide.ui.components

import formulaide.ui.utils.text
import kotlinx.html.INPUT
import kotlinx.html.InputType
import kotlinx.html.id
import kotlinx.html.js.onChangeFunction
import org.w3c.dom.HTMLInputElement
import react.RBuilder
import react.dom.attrs
import react.dom.div
import react.dom.input
import react.dom.label

fun RBuilder.styledInput(
	type: InputType,
	id: String,
	displayName: String,
	required: Boolean = false,
	onChangeFunction: (HTMLInputElement) -> Unit = {},
	handler: INPUT.() -> Unit = {},
) {
	styledFormField {
		label("block") {
			attrs["htmlFor"] = id

			text(displayName)

			if (required)
				text("*")
		}

		input(type,
		      classes = "rounded bg-gray-200 border-b-2 border-gray-400 focus:border-purple-800") {
			attrs {
				this.id = id
				this.name = id
				this.required = required

				this.onChangeFunction = {
					onChangeFunction(it.target as HTMLInputElement)
				}

				handler()
			}
		}
	}
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
