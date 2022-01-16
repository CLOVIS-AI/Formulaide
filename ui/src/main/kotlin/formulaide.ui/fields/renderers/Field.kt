package formulaide.ui.fields.renderers

import formulaide.api.data.Action
import formulaide.api.data.Form
import formulaide.api.fields.Field
import formulaide.ui.components.StyledButton
import react.FC
import react.Props
import react.dom.html.ReactHTML.div
import react.key
import react.useState
import formulaide.ui.components.inputs.Field as UIField

external interface FieldProps : Props {
	var form: Form?
	var root: Action?

	var field: Field
	var id: String?
	var fieldKey: String?

	var onInput: ((String, String) -> Unit)?
}

//region FieldProps Extensions

val FieldProps.idOrDefault get() = id ?: field.id
val FieldProps.fieldKeyOrDefault get() = fieldKey ?: idOrDefault

//endregion

/**
 * Displays a [Field] such that the user can fill it in.
 */
val Field: FC<FieldProps> = FC("Field") { props ->
	when {
		props.field.arity.max == 1 -> UniqueArityField { +props }
		props.field.arity.max > 1 -> ListArityField { +props }
		// else -> max arity is 0, the field is forbidden <=> there is nothing to display
	}
}

/**
 * Displays a [Field] that has a maximum [Field.arity] of 1.
 */
private val UniqueArityField: FC<FieldProps> = FC("UniqueArityField") { props ->
	UIField {
		id = props.idOrDefault
		text = props.field.name

		FieldRenderer {
			+props
			fieldKey = "${props.fieldKeyOrDefault}:${props.id}"
		}
	}
}

private val ListArityField: FC<FieldProps> = FC("ListArityField") { props ->
	val (fieldIds, setFieldIds) = useState(List(props.field.arity.min) { it })

	UIField {
		id = props.idOrDefault
		text = props.field.name

		for ((i, fieldId) in fieldIds.withIndex()) {
			div {
				key = fieldId.toString()

				FieldRenderer {
					+props
					id = "${props.idOrDefault}:$fieldId"
					fieldKey = fieldId.toString()
				}

				if (fieldIds.size > props.field.arity.min) {
					StyledButton {
						text = "×"
						action = {
							setFieldIds(
								fieldIds.subList(0, i) +
										fieldIds.subList(i + 1, fieldIds.size)
							)
						}
					}
				}
			}
		}

		if (fieldIds.size < props.field.arity.max) {
			StyledButton {
				text = "Ajouter une réponse"
				action = { setFieldIds(fieldIds + ((fieldIds.maxOrNull() ?: 0) + 1)) }
			}
		}
	}
}
