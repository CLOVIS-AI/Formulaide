package formulaide.ui.fields

import formulaide.api.data.*
import formulaide.api.fields.FormField
import formulaide.api.fields.SimpleField
import formulaide.ui.traceRenders
import formulaide.ui.utils.text
import react.*
import react.dom.div

fun RBuilder.immutableFields(answers: ParsedSubmission) {
	for (answer in answers.fields) {
		immutableField(answer)
	}
}

private fun RBuilder.immutableField(answer: ParsedField<*>) {
	child(ImmutableField) {
		attrs {
			this.answer = answer
		}
	}
}

private external interface ImmutableFieldProps : RProps {
	var answer: ParsedField<*>
}

private const val miniNesting = "px-4"

private val ImmutableField: FunctionComponent<ImmutableFieldProps> = fc { props ->
	traceRenders(props.answer.toString())

	div {
		when (val answer = props.answer) {
			is ParsedSimple<*> -> {
				if (answer.value !is SimpleField.Message) {
					text("${answer.constraint.name} : ${answer.value}")
				} // else: don't display messages here
			}
			is ParsedUnion<*, *> -> {
				val value = answer.value
				if (value is FormField.Simple && value.simple is SimpleField.Message) {
					text(answer.constraint.name + " : " + value.name)
				} else {
					text(answer.constraint.name)
					div(miniNesting) {
						immutableField(answer.children.first())
					}
				}
			}
			is ParsedList<*> -> {
				for (child in answer.children) {
					immutableField(child)
				}
			}
			is ParsedComposite<*> -> {
				val compositeField = answer.constraint

				text(compositeField.name)
				for (child in answer.children) {
					div(miniNesting) {
						immutableField(child)
					}
				}
			}
		}
	}
}
