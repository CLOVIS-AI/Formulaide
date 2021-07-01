package formulaide.ui.fields

import formulaide.api.fields.FormField
import formulaide.api.fields.SimpleField
import formulaide.api.types.Arity
import formulaide.ui.ScreenProps
import formulaide.ui.components.styledInput
import formulaide.ui.utils.text
import kotlinx.html.INPUT
import kotlinx.html.InputType
import kotlinx.html.js.onChangeFunction
import org.w3c.dom.HTMLInputElement
import react.*
import react.dom.*

private external interface FieldProps : RProps {
	var app: ScreenProps
	var field: FormField

	var id: String
}

private val Field: FunctionalComponent<FieldProps> = functionalComponent { props ->
	val field = props.field
	val required = field.arity == Arity.mandatory()

	val simpleInput = { type: InputType, _required: Boolean, handler: INPUT.() -> Unit ->
		styledInput(type, props.id, field.name, required = _required, handler = handler)
	}

	when (field) {
		is FormField.Simple -> when (field.simple) { //TODO: check validity of value
			is SimpleField.Text -> simpleInput(InputType.text, required) {}
			is SimpleField.Integer -> simpleInput(InputType.number, required) {}
			is SimpleField.Decimal -> simpleInput(InputType.number, required) {
				step = "any"
			}
			is SimpleField.Boolean -> simpleInput(InputType.checkBox, false) {}
			is SimpleField.Message -> p { text(field.name) }
		}
		is FormField.Composite -> {
			val subFields = field.fields

			div {
				attrs {
					jsStyle {
						marginLeft = "2rem"
					}
				}

				for (subField in subFields) {
					field(props.app, subField, "${props.id}:${subField.id}")
				}
			}
		}
		is FormField.Union<*> -> {
			val subFields = field.options
			val (selected, setSelected) = useState(subFields.first())

			div {
				attrs {
					jsStyle {
						marginLeft = "2rem"
					}
				}

				for (subField in subFields) {
					input(InputType.radio, name = "${props.id}:${subField.id}") {
						attrs {
							value = field.id
							checked = subField == selected

							onChangeFunction = { event ->
								setSelected(subFields.first { it.id == (event.target as HTMLInputElement).value })
							}
						}
					}
					label {
						text(subField.name)
						attrs { htmlFor = "${props.id}:${subField.id}" }
					}
				}

				field(props.app, selected, "${props.id}:${selected.id}")
			}
		}
	}

	//TODO: lists
}

fun RBuilder.field(
	app: ScreenProps,
	field: FormField,
	id: String? = null,
) = child(Field) {
	attrs {
		this.app = app
		this.field = field
		this.id = id ?: field.id
	}
}
