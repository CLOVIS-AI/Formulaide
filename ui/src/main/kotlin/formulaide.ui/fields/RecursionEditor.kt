package formulaide.ui.fields

import formulaide.api.fields.*
import formulaide.api.types.Arity
import formulaide.ui.components.styledButton
import formulaide.ui.components.styledFormField
import formulaide.ui.utils.replace
import formulaide.ui.utils.text
import react.FunctionComponent
import react.child
import react.fc

val RecursionEditor: FunctionComponent<EditableFieldProps> = fc { props ->
	val parent = props.field
	val fields = (parent as? Field.Union<*>)?.options
		?: (parent as? Field.Container<*>)?.fields

	if (fields != null) {
		styledFormField {
			when (parent) {
				is Field.Union<*> -> text("L'utilisateur doit choisir entre :")
				is Field.Container<*> -> text("L'utilisateur doit remplir :")
			}
		}

		for ((i, field) in fields.sortedBy { it.order }.withIndex()) {
			child(FieldEditor) {
				attrs {
					app = props.app
					this.field = field

					depth = props.depth + 1
					fieldNumber = i

					replace = { newField ->
						val newParent = when (parent) {
							is DataField.Union -> parent.copy(options = parent.options
								.replace(i, newField as DataField))
							is ShallowFormField.Union -> parent.copy(options = parent.options
								.replace(i, newField as ShallowFormField))
							is DeepFormField.Union -> parent.copy(options = parent.options
								.replace(i, newField as DeepFormField))
							is ShallowFormField.Composite -> parent.copy(fields = parent.fields
								.replace(i, newField as DeepFormField))
							is DeepFormField.Composite -> parent.copy(fields = parent.fields
								.replace(i, newField as DeepFormField))
							else -> error("Impossible de modifier les sous-champs de $parent")
						}

						props.replace(newParent)
					}
				}
			}
		}

		if (parent is DataField.Union || parent is ShallowFormField.Union) {
			styledButton("Ajouter une option") {
				val id = fields.size.toString()
				val order = fields.size
				val name = "Nouveau champ"
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
