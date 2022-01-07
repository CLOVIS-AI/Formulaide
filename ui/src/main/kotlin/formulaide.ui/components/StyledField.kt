package formulaide.ui.components

import formulaide.ui.components.text.Text
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLSelectElement
import react.ChildrenBuilder
import react.dom.html.InputHTMLAttributes
import react.dom.html.InputType
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.label
import react.dom.html.ReactHTML.select
import react.dom.html.SelectHTMLAttributes
import react.Ref as RRef

private const val commonInputStyle =
	"rounded bg-gray-200 border-b-2 border-gray-400 focus:border-purple-800 my-1 focus:outline-none"
private const val largeInputStyle = "$commonInputStyle w-60 max-w-full mr-3"
private const val smallInputStyle = "$commonInputStyle w-10 max-w-full"

fun ChildrenBuilder.styledField(
	id: String,
	displayName: String,
	contents: ChildrenBuilder.() -> Unit,
) {
	styledFormField {
		label {
			className = "block"
			htmlFor = id

			Text { text = displayName }
		}

		contents()
	}
}

private fun ChildrenBuilder.styledInputCommon(
	type: InputType,
	className: String,
	id: String,
	required: Boolean = false,
	ref: RRef<HTMLInputElement>? = null,
	handler: InputHTMLAttributes<HTMLInputElement>.() -> Unit = {},
) {
	input {
		this.type = type
		this.className = className
		this.id = id
		this.name = id
		this.required = required
		handler()

		if (ref != null) this.ref = ref
	}
	if (required)
		Text { text = " *" }
}

fun ChildrenBuilder.styledInput(
	type: InputType,
	id: String,
	required: Boolean = false,
	ref: RRef<HTMLInputElement>? = null,
	handler: InputHTMLAttributes<HTMLInputElement>.() -> Unit = {},
) = styledInputCommon(type, largeInputStyle, id, required, ref, handler)

fun ChildrenBuilder.styledSmallInput(
	type: InputType,
	id: String,
	required: Boolean = false,
	ref: RRef<HTMLInputElement>? = null,
	handler: InputHTMLAttributes<HTMLInputElement>.() -> Unit = {},
) = styledInputCommon(type, smallInputStyle, id, required, ref, handler)

fun ChildrenBuilder.styledFormField(contents: ChildrenBuilder.() -> Unit) {
	div {
		className = "mb-2"
		contents()
	}
}

fun ChildrenBuilder.styledRadioButton(
	radioId: String,
	buttonId: String,
	value: String,
	text: String,
	checked: Boolean = false,
	onClick: () -> Unit = {},
) {
	input {
		this.type = InputType.radio
		name = radioId
		className = "mr-1"
		id = buttonId
		this.value = value

		onChange = { onClick() }
		this.checked = checked
	}

	label {
		className = "mr-2"

		Text { this.text = text }
		htmlFor = buttonId
	}
}

fun ChildrenBuilder.styledCheckbox(
	id: String,
	message: String,
	required: Boolean = false,
	ref: RRef<HTMLInputElement>? = null,
	handler: InputHTMLAttributes<HTMLInputElement>.() -> Unit = {},
) {
	input {
		type = InputType.hidden
		name = id
		value = "false"
	}

	styledSmallInput(InputType.checkbox, id, required, ref) {
		value = "true"
		handler()
	}
	label {
		Text { text = message }
		htmlFor = id
	}
}

fun ChildrenBuilder.styledSelect(
	handler: SelectHTMLAttributes<HTMLSelectElement>.() -> Unit = {},
	onSelect: (HTMLSelectElement) -> Unit = {},
	contents: SelectHTMLAttributes<HTMLSelectElement>.() -> Unit,
) {
	select {
		className = largeInputStyle
		onChange = { onSelect(it.target) }

		handler()

		contents()
	}
}
