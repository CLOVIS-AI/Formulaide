package formulaide.ui.fields

import formulaide.api.fields.Field
import formulaide.api.fields.SimpleField
import formulaide.api.types.Arity
import formulaide.ui.utils.text
import kotlinx.html.InputType
import kotlinx.html.js.onChangeFunction
import org.w3c.dom.HTMLInputElement
import react.dom.attrs
import react.dom.input
import react.dom.label
import react.functionalComponent

val ArityEditor = functionalComponent<EditableFieldProps> { props ->
	val field = props.field
	val arity = field.arity

	//region Allowed
	// Editing the arity is allowed for all types but Message
	val simpleOrNull = (field as? Field.Simple)?.simple
	val allowModifications =
		if (simpleOrNull != null) simpleOrNull::class != SimpleField.Message::class
		else true
	//endregion

	if (allowModifications) {
		label { text("Nombre de réponses autorisées : de ") }
		input(InputType.number, name = "item-arity-min") {
			attrs {
				required = true
				value = arity.min.toString()
				min = "0"
				max = arity.max.toString()
				onChangeFunction = {
					val value = (it.target as HTMLInputElement).value.toInt()
					props.replace(
						props.field.set(arity = Arity(value, arity.max))
					)
				}
			}
		}
		label { text(" à ") }
		input(InputType.number, name = "item-arity-max") {
			attrs {
				required = true
				value = arity.max.toString()
				min = arity.min.toString()
				max = "1000"
				onChangeFunction = {
					val value = (it.target as HTMLInputElement).value.toInt()
					props.replace(
						props.field.set(arity = Arity(arity.min, value))
					)
				}
			}
		}
	} else {
		label { text("Nombre de réponses autorisées : de ${arity.min} à ${arity.max} ") }
	}
	when (arity) {
		Arity.mandatory() -> label { text("(obligatoire)") }
		Arity.optional() -> label { text("(facultatif)") }
		Arity.forbidden() -> label { text("(interdit)") }
		else -> label { text("(liste)") }
	}
}
