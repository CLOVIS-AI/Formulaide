package formulaide.ui.fields

import formulaide.api.fields.Field
import formulaide.api.fields.FormField
import formulaide.api.fields.SimpleField
import formulaide.api.types.Arity
import formulaide.ui.ScreenProps
import formulaide.ui.components.*
import kotlinx.html.INPUT
import kotlinx.html.InputType
import react.*

private external interface FieldProps : RProps {
	var app: ScreenProps
	var field: FormField

	var id: String
}

private val RenderField = functionalComponent<FieldProps> { props ->
	val field = props.field
	val required = field.arity == Arity.mandatory()

	val simpleInput = { type: InputType, _required: Boolean, handler: INPUT.() -> Unit ->
		styledInput(type, props.id, required = _required, handler = handler)
	}

	when (field) {
		is FormField.Simple -> when (field.simple) { //TODO: check validity of value
			is SimpleField.Text -> simpleInput(InputType.text, required) {}
			is SimpleField.Integer -> simpleInput(InputType.number, required) {}
			is SimpleField.Decimal -> simpleInput(InputType.number, required) {
				step = "any"
			}
			is SimpleField.Boolean -> simpleInput(InputType.checkBox, false) {}
			is SimpleField.Message -> Unit // The message has already been displayed
		}
		is FormField.Composite -> {
			val subFields = field.fields

			styledNesting {
				for (subField in subFields) {
					field(props.app, subField, "${props.id}:${subField.id}")
				}
			}
		}
		is FormField.Union<*> -> {
			val subFields = field.options
			val (selected, setSelected) = useState(subFields.first())

			styledNesting {
				styledFormField {
					for (subField in subFields) {
						styledRadioButton(
							radioId = props.id,
							buttonId = "${props.id}-${subField.id}",
							value = subField.id,
							text = subField.name,
							checked = subField == selected,
							onClick = { setSelected(subField) }
						)
					}
				}

				if (selected !is Field.Simple || selected.simple != SimpleField.Message) {
					field(props.app, selected, "${props.id}:${selected.id}")
				}
			}
		}
	}
}

private val Field: FunctionalComponent<FieldProps> = functionalComponent { props ->

	if (props.field.arity != Arity.forbidden()) {
		styledField(props.id, props.field.name) {
			child(RenderField) {
				attrs {
					this.app = props.app
					this.field = props.field
					this.id = props.id
				}
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
