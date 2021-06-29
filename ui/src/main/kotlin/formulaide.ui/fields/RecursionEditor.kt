package formulaide.ui.fields

import formulaide.api.fields.DataField
import formulaide.api.fields.Field
import formulaide.api.fields.FormField
import formulaide.ui.utils.replace
import formulaide.ui.utils.text
import react.FunctionalComponent
import react.child
import react.dom.attrs
import react.dom.div
import react.dom.jsStyle
import react.functionalComponent

val RecursionEditor: FunctionalComponent<FieldProps2> = functionalComponent { props ->
	val parent = props.field
	val fields = (parent as? Field.Union<*>)?.options
		?: (parent as? Field.Container<*>)?.fields

	if (fields != null) {
		when (parent) {
			is Field.Union<*> -> text("L'utilisateur doit choisir entre :")
			is Field.Container<*> -> text("L'utilisateur doit remplir :")
		}

		div {
			attrs {
				jsStyle { marginLeft = "2rem" }
			}

			for ((i, field) in fields.withIndex()) {
				child(FieldEditor) {
					attrs {
						app = props.app
						this.field = field
						replace = { newField ->
							val newParent = when (parent) {
								is DataField.Union -> parent.copy(options = parent.options.replace(i, newField as DataField))
								is FormField.Shallow.Union -> parent.copy(options = parent.options.replace(i, newField as FormField.Shallow))
								is FormField.Deep.Union -> parent.copy(options = parent.options.replace(i, newField as FormField.Deep))
								is FormField.Shallow.Composite -> parent.copy(fields = parent.fields.replace(i, newField as FormField.Deep))
								is FormField.Deep.Composite -> parent.copy(fields = parent.fields.replace(i, newField as FormField.Deep))
								else -> error("Impossible de modifier les sous-champs de $parent")
							}

							props.replace(newParent)
						}
					}
				}
			}
		}
	}
}
