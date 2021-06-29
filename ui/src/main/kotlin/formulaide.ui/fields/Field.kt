package formulaide.ui.fields

import formulaide.api.fields.FormField
import formulaide.api.fields.SimpleField
import formulaide.api.types.Arity
import formulaide.ui.ScreenProps
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

	br {}
	label {
		text(field.name)

		if (field.arity == Arity.mandatory())
			text("*")
	}

	val configureName: RDOMBuilder<INPUT>.() -> Unit = {
		attrs {
			name = props.id
		}
	}

	val configureRequired: RDOMBuilder<INPUT>.() -> Unit = {
		attrs {
			required = field.arity == Arity.mandatory()
		}
	}

	when (field) {
		is FormField.Simple -> when (field.simple) { //TODO: check validity of value
			is SimpleField.Text -> input(InputType.text) {
				configureName()
				configureRequired()
			}
			is SimpleField.Integer -> input(InputType.number) {
				configureName()
				configureRequired()
			}
			is SimpleField.Message -> Unit // Nothing to do, see MESSAGE
			is SimpleField.Decimal -> input(InputType.number) {
				configureName()
				configureRequired()
				attrs {
					step = "any"
				}
			}
			is SimpleField.Boolean -> input(InputType.checkBox) {
				configureName()
			}
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
