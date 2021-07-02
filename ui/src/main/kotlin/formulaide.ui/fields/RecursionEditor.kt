package formulaide.ui.fields

import formulaide.api.fields.DataField
import formulaide.api.fields.Field
import formulaide.api.fields.FormField
import formulaide.api.fields.SimpleField
import formulaide.api.types.Arity
import formulaide.ui.components.styledButton
import formulaide.ui.components.styledFormField
import formulaide.ui.utils.replace
import formulaide.ui.utils.text
import react.FunctionalComponent
import react.child
import react.functionalComponent

val RecursionEditor: FunctionalComponent<EditableFieldProps> = functionalComponent { props ->
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

		for ((i, field) in fields.withIndex()) {
			child(FieldEditor) {
				attrs {
					app = props.app
					this.field = field
					replace = { newField ->
						val newParent = when (parent) {
							is DataField.Union -> parent.copy(options = parent.options
								.replace(i, newField as DataField))
							is FormField.Shallow.Union -> parent.copy(options = parent.options
								.replace(i, newField as FormField.Shallow))
							is FormField.Deep.Union -> parent.copy(options = parent.options
								.replace(i, newField as FormField.Deep))
							is FormField.Shallow.Composite -> parent.copy(fields = parent.fields
								.replace(i, newField as FormField.Deep))
							is FormField.Deep.Composite -> parent.copy(fields = parent.fields
								.replace(i, newField as FormField.Deep))
							else -> error("Impossible de modifier les sous-champs de $parent")
						}

						props.replace(newParent)
					}
				}
			}
		}

		if (parent is DataField.Union || parent is DataField.Composite || parent is FormField.Shallow.Union) {
			styledButton("Ajouter un champ") {
				val id = fields.size.toString()
				val order = fields.size
				val name = "Nouveau champ"
				val simple = SimpleField.Text(Arity.optional())

				val newParent = when (parent) {
					is DataField.Union -> parent.copy(options = parent.options +
							DataField.Simple(id, order, name, simple))
					is FormField.Shallow.Union -> parent.copy(options = parent.options +
							FormField.Shallow.Simple(id, order, name, simple))
					else -> error("Impossible d'ajouter un sous-champ à $parent")
				}

				props.replace(newParent)
			}
		}
	}
}
