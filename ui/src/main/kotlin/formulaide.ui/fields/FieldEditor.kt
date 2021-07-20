package formulaide.ui.fields

import formulaide.api.fields.*
import formulaide.api.types.Arity
import formulaide.ui.ScreenProps
import formulaide.ui.components.styledFieldEditorShell
import formulaide.ui.components.styledFormField
import formulaide.ui.utils.text
import react.RProps
import react.child
import react.fc

external interface EditableFieldProps : RProps {
	var app: ScreenProps

	var field: Field
	var replace: (Field) -> Unit
}

fun EditableFieldProps.inheritFrom(props: EditableFieldProps) {
	app = props.app
	field = props.field
	replace = props.replace
}

@Suppress("NAME_SHADOWING")
internal fun SimpleField.set(arity: Arity? = null): SimpleField {
	val arity = arity ?: this.arity

	return when (this) {
		is SimpleField.Text -> copy(arity = arity)
		is SimpleField.Integer -> copy(arity = arity)
		is SimpleField.Decimal -> copy(arity = arity)
		is SimpleField.Boolean -> copy(arity = arity)
		is SimpleField.Email -> copy(arity = arity)
		is SimpleField.Date -> copy(arity = arity)
		is SimpleField.Time -> copy(arity = arity)
		SimpleField.Message -> this
	}
}

@Suppress("NAME_SHADOWING")
internal fun Field.set(name: String? = null, arity: Arity? = null): Field {
	val name = name ?: this.name
	val arity = arity ?: this.arity

	return when (this) {
		// Data fields
		is DataField.Simple -> copy(name = name, simple = simple.set(arity = arity))
		is DataField.Union -> copy(name = name, arity = arity)
		is DataField.Composite -> copy(name = name, arity = arity)

		// Shallow form fields
		is ShallowFormField.Simple -> copy(name = name, simple = simple.set(arity = arity))
		is ShallowFormField.Union -> copy(name = name, arity = arity)
		is ShallowFormField.Composite -> copy(name = name, arity = arity)

		// Deep form fields: the name cannot be changed
		is DeepFormField.Simple -> copy(simple = simple.set(arity = arity))
		is DeepFormField.Union -> copy(arity = arity)
		is DeepFormField.Composite -> copy(arity = arity)

		else -> error("Le type de ce champ n'est pas géré : ${this::class}, $this")
	}
}

val FieldEditor = fc<EditableFieldProps> { props ->
	styledFieldEditorShell("item-editor-${props.field.id}", "Champ ${props.field.order}") {

		child(NameEditor) {
			attrs { inheritFrom(props) }
		}

		child(TypeEditor) {
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
				styledFormField { text("Les sous-champs ne sont pas affichés, parce que cette donnée est absente.") }
			}
		}
	}
}
