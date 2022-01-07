package formulaide.ui.components.inputs

import formulaide.ui.components.text.Text
import org.w3c.dom.HTMLInputElement
import react.FC
import react.dom.html.InputHTMLAttributes
import react.dom.html.ReactHTML.input

private const val commonInputStyle =
	"rounded bg-gray-200 border-b-2 border-gray-400 focus:border-purple-800 my-1 focus:outline-none"
internal const val largeInputStyle = "$commonInputStyle w-60 max-w-full mr-3"
internal const val smallInputStyle = "$commonInputStyle w-10 max-w-full"

typealias InputProps = InputHTMLAttributes<HTMLInputElement>

val CommonInput = FC<InputProps>("CommonInput") { props ->
	input {
		this.name = props.id

		+props
	}

	if (props.required == true)
		Text { text = " *" }
}

val Input = FC<InputProps>("Input") { props ->
	CommonInput {
		className = largeInputStyle
		+props
	}
}

val SmallInput = FC<InputProps>("SmallInput") { props ->
	CommonInput {
		className = smallInputStyle
		+props
	}
}
