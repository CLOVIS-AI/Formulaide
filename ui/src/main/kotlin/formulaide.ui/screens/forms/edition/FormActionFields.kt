package formulaide.ui.screens.forms.edition

import formulaide.api.fields.Field
import formulaide.api.fields.FormRoot
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
import react.key

val FormActionFields = FC<FormActionProps>("FormActionFields") { props ->
	val action = props.action
	val replace = props.onReplace
	val maxFieldId = props.maxFieldId
	val root = action.fields ?: FormRoot(emptyList())

	val lambdas = useLambdas()

	Field {
		id = "new-form-action-${action.id}-fields"
		text = "Champs réservés à l'administration"

		for ((i, field) in root.fields.withIndex()) {
			FieldEditor {
				this.field = field
				key = field.id
				uniqueId = "action-${action.id}:${field.id}"

				this.replace = { it: Field ->
					val newFields = root.fields.replace(i, it as ShallowFormField)
					replace(action.copy(fields = FormRoot(newFields)))
				}.memoIn(lambdas, "action-fields-replace-${field.id}", i, root)

				this.remove = {
					val newFields = root.fields.remove(i)
					replace(action.copy(fields = FormRoot(newFields)))
				}.memoIn(lambdas, "action-fields-remove-${field.id}", i, root)

				this.switch = { direction: SwitchDirection ->
					val newFields = root.fields.switchOrder(i, direction)
					replace(action.copy(fields = FormRoot(newFields)))
				}.memoIn(lambdas, "action-fields-switch-${field.id}", i, root)

				depth = 1
				fieldNumber = i
			}
		}

		StyledButton {
			text = "Ajouter un champ"
			this.action = {
				val newFields = root.fields + ShallowFormField.Simple(
					maxFieldId.toString(),
					root.fields.size,
					"",
					SimpleField.Text(Arity.mandatory()),
				)

				replace(action.copy(fields = FormRoot(newFields)))
			}
		}
	}
}
