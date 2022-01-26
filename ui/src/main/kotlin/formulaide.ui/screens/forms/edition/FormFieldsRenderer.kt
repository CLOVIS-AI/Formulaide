package formulaide.ui.screens.forms.edition

import formulaide.api.fields.Field
import formulaide.api.fields.ShallowFormField
import formulaide.api.fields.SimpleField
import formulaide.api.types.Arity
import formulaide.ui.components.StyledButton
import formulaide.ui.components.inputs.Field
import formulaide.ui.components.memoIn
import formulaide.ui.components.useLambdas
import formulaide.ui.fields.editors.FieldEditor
import formulaide.ui.fields.editors.SwitchDirection
import formulaide.ui.utils.remove
import formulaide.ui.utils.replace
import formulaide.ui.utils.switchOrder
import react.FC
import react.Props
import react.key
import react.useMemo

external interface FormFieldsRendererProps : Props {
	var fields: List<ShallowFormField>
	var updateFields: (List<ShallowFormField>.() -> List<ShallowFormField>) -> Unit
}

val FormFieldsRenderer = FC<FormFieldsRendererProps>("FormFieldsRenderer") { props ->
	val fields = props.fields

	val lambdas = useLambdas()
	val maxFieldId = useMemo(fields) { fields.maxOfOrNull { it.id.toInt() }?.plus(1) ?: 0 }

	Field {
		id = "new-form-fields"
		text = "Champs"

		for ((i, field) in fields.withIndex()) {
			FieldEditor {
				this.field = field
				key = field.id
				uniqueId = "initial:${field.id}"

				replace = { it: Field ->
					props.updateFields { replace(i, it as ShallowFormField) }
				}.memoIn(lambdas, "replace-${field.id}", i)

				this.remove = {
					props.updateFields { remove(i) }
				}.memoIn(lambdas, "remove-${field.id}", i)

				switch = { direction: SwitchDirection ->
					props.updateFields { switchOrder(i, direction) }
				}.memoIn(lambdas, "switch-${field.id}", i)

				depth = 0
				fieldNumber = i
			}
		}

		StyledButton {
			text = "Ajouter un champ"
			action = {
				props.updateFields {
					this + ShallowFormField.Simple(
						order = size,
						id = maxFieldId.toString(),
						name = "",
						simple = SimpleField.Text(Arity.optional())
					)
				}
			}
		}
	}
}
