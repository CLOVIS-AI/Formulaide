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

		input(type, classes = "border focus:border-4") {
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
