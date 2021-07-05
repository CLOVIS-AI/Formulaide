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
import react.dom.attrs
import react.dom.div

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
			is SimpleField.Email -> simpleInput(InputType.email, required) {}
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

	if (props.field.arity.max == 1) {
		styledField(props.id, props.field.name) {
			child(RenderField) {
				attrs {
					this.app = props.app
					this.field = props.field
					this.id = props.id
				}
			}
		}
	} else if (props.field.arity.max > 1) {
		val (fieldIds, setFieldIds) = useState(List(props.field.arity.min) { it })

		styledField(props.id, props.field.name) {
			for ((i, fieldId) in fieldIds.withIndex()) {
				div {
					child(RenderField) {
						attrs {
							this.app = props.app
							this.field = props.field
							this.id = "${props.id}:$fieldId"
						}
					}
					if (fieldIds.size > props.field.arity.min) {
						styledButton("×") {
							setFieldIds(
								fieldIds.subList(0, i) +
										fieldIds.subList(i + 1, fieldIds.size)
							)
						}
					}

					attrs {
						key = fieldId.toString()
					}
				}
			}
			if (fieldIds.size < props.field.arity.max) {
				styledButton("Ajouter une réponse") {
					setFieldIds(fieldIds + ((fieldIds.maxOrNull() ?: 0) + 1))
				}
			}
		}
	} // else: max arity is 0, the field is forbidden, so there is nothing to display
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
