package formulaide.ui.fields

import formulaide.api.fields.DataField
import formulaide.api.fields.Field
import formulaide.api.types.Arity
import formulaide.ui.components.styledFormField
import formulaide.ui.components.styledNesting
import formulaide.ui.utils.text
import react.RProps
import react.child
import react.dom.p
import react.fc
import react.memo

enum class SwitchDirection(val offset: Int) {
	UP(-1),
	DOWN(1);
}

external interface EditableFieldProps : RProps {
	var field: Field
	var replace: (Field) -> Unit
	var remove: () -> Unit
	var switch: (SwitchDirection) -> Unit

	var depth: Int
	var fieldNumber: Int
}

fun EditableFieldProps.inheritFrom(props: EditableFieldProps) {
	field = props.field
	replace = props.replace
	remove = props.remove
	depth = props.depth
	fieldNumber = props.fieldNumber
	switch = props.switch
}

val FieldEditor = memo(fc<EditableFieldProps> { props ->
	styledNesting(
		props.depth, props.fieldNumber,
		onDeletion = { props.remove() },
		onMoveUp = { props.switch(SwitchDirection.UP) },
		onMoveDown = { props.switch(SwitchDirection.DOWN) },
	) {

		child(NameEditor) {
			attrs { inheritFrom(props) }
		}

		child(TypeEditor) {
			attrs { inheritFrom(props) }
		}

		child(MetadataEditor) {
			attrs { inheritFrom(props) }
		}

		child(ArityEditor) {
			attrs { inheritFrom(props) }
		}

		if (props.field is Field.Union<*> || props.field is Field.Container<*>) {
			if (props.field.arity != Arity.forbidden()) {
				child(RecursionEditor) {
					attrs { inheritFrom(props) }
				}
			} else {
				styledFormField { text("Les sous-champs ne sont pas affichés, parce que cette donnée est cachée.") }
			}
		} else if (props.field is DataField.Composite) {
			styledFormField {
				p { text("Les sous-champs ne sont modifiables que pendant la création d'un formulaire.") }
				p { text("Un groupe à l'intérieur d'un autre groupe ne peut pas être obligatoire.") }
			}
		}
	}
})
