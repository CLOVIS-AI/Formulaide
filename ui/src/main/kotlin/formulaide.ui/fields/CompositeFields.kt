package formulaide.ui.fields

import formulaide.api.fields.DataField
import formulaide.api.fields.Field
import formulaide.api.fields.SimpleField
import formulaide.api.types.Arity
import formulaide.ui.ScreenProps
import formulaide.ui.utils.text
import react.RProps
import react.child
import react.dom.br
import react.dom.div
import react.functionalComponent

external interface FieldProps2 : RProps {
	var app: ScreenProps

	var field: Field
	var replace: (Field) -> Unit
}

fun FieldProps2.inheritFrom(props: FieldProps2) {
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
		SimpleField.Message -> error("Modifier l'arité d'un Message est interdit")
	}
}

@Suppress("NAME_SHADOWING")
internal fun Field.set(name: String? = null, arity: Arity? = null): Field {
	val name = name ?: this.name
	val arity = arity ?: this.arity

	return when (this) {
		is DataField.Simple -> copy(name = name, simple = simple.set(arity = arity))
		is DataField.Union -> copy(name = name, arity = arity)
		is DataField.Composite -> copy(name = name, arity = arity)
		else -> TODO("Le type de champ ${this::class} n'est pas géré")
	}
}

val FieldSimple = functionalComponent<FieldProps2> { props ->
	div {
		text("Champ ${props.field.order}")

		br {}
		child(NameEditor) {
			attrs { inheritFrom(props) }
		}

		br {}
		child(ArityEditor) {
			attrs { inheritFrom(props) }
		}

		br {}
		//TODO: edit the type
	}
}
