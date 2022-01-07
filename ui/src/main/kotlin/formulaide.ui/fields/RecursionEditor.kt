package formulaide.ui.fields

import formulaide.api.fields.*
import formulaide.api.types.Arity
import formulaide.ui.components.StyledButton
import formulaide.ui.components.inputs.FormField
import formulaide.ui.utils.remove
import formulaide.ui.utils.replace
import formulaide.ui.utils.switchOrder
import react.FC
import react.key
import react.useMemo

val RecursionEditor: FC<EditableFieldProps> = FC("RecursionEditor") { props ->
	val parent = props.field
	val fields = (parent as? Field.Union<*>)?.options?.sortedBy { it.order }
		?: (parent as? Field.Container<*>)?.fields?.sortedBy { it.order }

	val maxId =
		useMemo(fields) { (fields ?: emptyList()).maxOfOrNull { it.id.toInt() }?.plus(1) ?: 0 }

	if (fields != null) {
		FormField {
			when (parent) {
				is Field.Union<*> -> +"L'utilisateur doit choisir entre :"
				is Field.Container<*> -> +"L'utilisateur doit remplir :"
			}
		}

		for ((i, field) in fields.withIndex()) {
			FieldEditor {
				this.field = field
				key = field.id
				uniqueId = "${props.uniqueId}:${field.id}"

				depth = props.depth + 1
				fieldNumber = i

				replace = { newField ->
					@Suppress("UNCHECKED_CAST")
					val newParent = when (parent) {
						is DataField.Union -> parent.copy(options = (fields as List<DataField>)
							.replace(i, newField as DataField))
						is ShallowFormField.Union -> parent.copy(options = (fields as List<ShallowFormField>)
							.replace(i, newField as ShallowFormField))
						is DeepFormField.Union -> parent.copy(options = (fields as List<DeepFormField>)
							.replace(i, newField as DeepFormField))
						is ShallowFormField.Composite -> parent.copy(fields = (fields as List<DeepFormField>)
							.replace(i, newField as DeepFormField))
						is DeepFormField.Composite -> parent.copy(fields = (fields as List<DeepFormField>)
							.replace(i, newField as DeepFormField))
						else -> error("Impossible de modifier les sous-champs de $parent")
					}

					props.replace(newParent)
				}

				remove = {
					val newParent = when (parent) {
						is DataField.Union ->
							parent.copy(options = parent.options.remove(i))
						is ShallowFormField.Union ->
							parent.copy(options = parent.options.remove(i))
						is DeepFormField.Union ->
							parent.copy(options = parent.options.remove(i))
						is ShallowFormField.Composite ->
							parent.copy(fields = parent.fields.remove(i))
						is DeepFormField.Composite ->
							parent.copy(fields = parent.fields.remove(i))
						else -> error("Impossible de modifier les sous-champs de $parent")
					}

					props.replace(newParent)
				}

				if (parent is DataField.Union || parent is ShallowFormField.Union) {
					switch = { direction ->
						val newParent = when (parent) {
							is DataField.Union ->
								parent.copy(options = parent.options.switchOrder(i, direction))
							is ShallowFormField.Union ->
								parent.copy(options = parent.options.switchOrder(i, direction))
							is DeepFormField.Union ->
								parent.copy(options = parent.options.switchOrder(i, direction))
							is ShallowFormField.Composite ->
								parent.copy(fields = parent.fields.switchOrder(i, direction))
							is DeepFormField.Composite ->
								parent.copy(fields = parent.fields.switchOrder(i, direction))
							else -> error("Impossible de modifier les sous-champs de $parent")
						}

						props.replace(newParent)
					}
				}
			}
		}

		if (parent is DataField.Union || parent is ShallowFormField.Union) {
			StyledButton {
				text = "Ajouter une option"
				action = {
					val id = maxId.toString()
					val order = fields.size
					val name = ""
					val simple = SimpleField.Text(Arity.optional())

					val newParent = when (parent) {
						is DataField.Union -> parent.copy(options = parent.options +
								DataField.Simple(id, order, name, simple))
						is ShallowFormField.Union -> parent.copy(options = parent.options +
								ShallowFormField.Simple(id, order, name, simple))
						else -> error("Impossible d'ajouter un sous-champ à $parent")
					}

					props.replace(newParent)
				}
			}
		}
	}
}
