package formulaide.ui.fields

import formulaide.api.data.CompoundData
import formulaide.api.data.CompoundDataField
import formulaide.api.data.Data
import formulaide.api.data.FormField
import formulaide.api.types.Arity
import formulaide.ui.utils.text
import kotlinx.html.InputType
import kotlinx.html.id
import kotlinx.html.js.onChangeFunction
import org.w3c.dom.HTMLInputElement
import react.RBuilder
import react.RProps
import react.child
import react.dom.*
import react.functionalComponent

external interface EditableFieldProps : RProps {
	var order: Int

	var arity: Arity
	var setArity: (Arity) -> Unit

	var name: String
	var setName: (String) -> Unit

	var data: Data
	var setDataType: (Data) -> Unit

	var compounds: List<CompoundData>
}

/**
 * A component that handles the settings of a field.
 *
 * Use it to create [CompoundDataField], [FormField]…
 */
val EditableField = functionalComponent<EditableFieldProps> { props ->
	div {
		text("Champ ${props.order} :")

		br {}
		label { text("Nom") }
		input(InputType.text, name = "item-name") {
			attrs {
				required = true
				placeholder = "Nom du champ"
				onChangeFunction = { props.setName((it.target as HTMLInputElement).value) }
			}
		}

		br {}
		label { text("Nombre de réponses autorisées : de ") }
		input(InputType.number, name = "item-arity-min") {
			attrs {
				required = true
				value = props.arity.min.toString()
				min = "0"
				max = props.arity.max.toString()
				onChangeFunction = {
					props.setArity(
						Arity(
							(it.target as HTMLInputElement).value.toInt(),
							props.arity.max
						)
					)
				}
			}
		}
		label { text(" à ") }
		input(InputType.number, name = "item-arity-max") {
			attrs {
				required = true
				value = props.arity.max.toString()
				min = props.arity.min.toString()
				max = "1000"
				onChangeFunction = {
					props.setArity(
						Arity(
							props.arity.min,
							(it.target as HTMLInputElement).value.toInt()
						)
					)
				}
			}
		}
		when (props.arity) {
			Arity.mandatory() -> label { text("(obligatoire)") }
			Arity.optional() -> label { text("(facultatif)") }
			Arity.forbidden() -> label { text("(interdit)") }
			else -> label { text("(liste)") }
		}

		br {}
		label { text("Type") }
		select {
			for (compound in props.compounds)
				option {
					text(compound.name)
					attrs {
						value = compound.id
					}
				}
			for (simple in Data.Simple.SimpleDataId.values())
				option {
					text(simple.displayName)
					attrs {
						value = "simple:${simple.ordinal}"
					}
				}
			//TODO: handle unions

			attrs {
				id = "data-list-${props.order}"
				required = true

				value =
					when (props.data) {
						is Data.Simple -> "simple:${(props.data as Data.Simple).id}"
						is Data.Compound -> (props.data as Data.Compound).id
						else -> TODO("Handle unions")
					}

				onChangeFunction = { event ->
					val value = (event.target as HTMLInputElement).value
					if (value.startsWith("simple:")) {
						val id = value.substringAfter(':').toInt()
						props.setDataType(
							Data.simple(
								Data.Simple.SimpleDataId.values().find { it.ordinal == id }
									?: error("Aucune donnée simple n'a l'ordinal $id, c'est impossible")))
					} else {
						props.setDataType(Data.compoundById(value))
					}
				}
			}
		}
	}
}

fun RBuilder.editableField(handler: EditableFieldProps.() -> Unit) = child(EditableField) {
	attrs {
		handler()
	}
}
