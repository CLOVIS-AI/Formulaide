package formulaide.ui.fields

import formulaide.api.fields.DataField
import formulaide.api.fields.DeepFormField
import formulaide.api.fields.Field
import formulaide.api.types.Arity
import formulaide.ui.components.inputs.FormField
import formulaide.ui.components.inputs.Nesting
import formulaide.ui.components.text.Text
import react.FC
import react.Props
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.p
import react.memo

enum class SwitchDirection(val offset: Int) {
	UP(-1),
	DOWN(1);
}

external interface EditableFieldProps : Props {
	var field: Field
	var uniqueId: String
	var replace: (Field) -> Unit
	var remove: () -> Unit
	var switch: (SwitchDirection) -> Unit

	var depth: Int
	var fieldNumber: Int
}

val FieldEditor = memo(FC<EditableFieldProps>("FieldEditor") { props ->
	Nesting {
		depth = props.depth
		fieldNumber = props.fieldNumber

		onDeletion = suspend { props.remove() }
			.takeIf { props.field !is DeepFormField }
		onMoveUp = suspend { props.switch(SwitchDirection.UP) }
			.takeIf { props.field !is DeepFormField }
		onMoveDown = suspend { props.switch(SwitchDirection.DOWN) }
			.takeIf { props.field !is DeepFormField }

		div {
			className = "flex gap-x-16 flex-wrap"

			div {
				NameEditor { +props }
				TypeEditor { +props }
			}

			div {
				className = "mr-32"

				MetadataEditor { +props }
			}
		}

		ArityEditor { +props }

		if (props.field is Field.Union<*> || props.field is Field.Container<*>) {
			if (props.field.arity != Arity.forbidden()) {
				RecursionEditor { +props }
			} else {
				FormField {
					Text {
						text = "Les sous-champs ne sont pas affichés, parce que cette donnée est cachée."
					}
				}
			}
		} else if (props.field is DataField.Composite) {
			FormField {
				p { Text { text = "Les sous-champs ne sont modifiables que pendant la création d'un formulaire." } }
				p { Text { text = "Un groupe à l'intérieur d'un autre groupe ne peut pas être obligatoire." } }
			}
		}
	}
})
