package formulaide.ui.fields

import formulaide.api.data.*
import formulaide.api.types.Arity
import formulaide.ui.utils.text
import kotlinx.html.InputType
import kotlinx.html.id
import kotlinx.html.js.onChangeFunction
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLSelectElement
import react.*
import react.dom.*

external interface EditableFieldProps : RProps {
	var order: Int
	var arity: Arity
	var name: String
	var data: Data
	var compounds: List<CompoundData>

	/**
	 * Does this field allow sub-fields?
	 */
	var recursive: Boolean

	/**
	 * When `false`, editing the [name] and the [data] is forbidden.
	 */
	var allowModifications: Boolean

	/**
	 * Allows to send requests for modifications to the parent Component.
	 * In order of appearance:
	 * - [String] is [name]
	 * - [Data] is [data]
	 * - [Int] is [arity].[min][Arity.min]
	 * - [Int] is [arity].[max][Arity.max]
	 * - [List] is subfields (only used when [recursive]` == true`)
	 *
	 * `null` means 'no changes'.
	 */
	var set: ((String?, Data?, Int?, Int?, List<FormFieldComponent>?) -> Unit)
}

/**
 * A component that handles the settings of a field.
 *
 * Use it to create [CompoundDataField], [FormField]…
 */
val EditableField: FunctionalComponent<EditableFieldProps> = functionalComponent { props ->
	val (subFields, _setSubFields) = useState(emptyList<Pair<FormFieldComponent, CompoundDataField>>())
	fun setSubFields(fields: List<Pair<FormFieldComponent, CompoundDataField>>): List<FormFieldComponent> {
		_setSubFields(fields)
		return fields.map { it.first }
	}

	fun replaceField(
		index: Int,
		value: FormFieldComponent,
		from: CompoundDataField
	): List<FormFieldComponent> =
		setSubFields(
			subFields.subList(0, index) + (value to from) + subFields.subList(
				index + 1,
				subFields.size
			)
		)

	div {
		text("Champ ${props.order} :")

		br {}
		if (props.allowModifications) {
			label { text("Nom") }
			input(InputType.text, name = "item-name") {
				attrs {
					required = true
					placeholder = "Nom du champ"
					onChangeFunction =
						{ props.set((it.target as HTMLInputElement).value, null, null, null, null) }
				}
			}
		} else {
			text(" ${props.name}")
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
					props.set(null, null, (it.target as HTMLInputElement).value.toInt(), null, null)
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
					props.set(null, null, null, (it.target as HTMLInputElement).value.toInt(), null)
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
		if (props.allowModifications) {
			label { text("Type") }
			select {
				for (compound in props.compounds)
					option {
						text(compound.name)
						attrs {
							value = compound.id

							val selectedData = props.data
							if (selectedData is Data.Compound)
								if (selectedData.id == compound.id)
									selected = true
						}
					}
				for (simple in Data.Simple.SimpleDataId.values())
					option {
						text(simple.displayName)
						attrs {
							value = "simple:${simple.ordinal}"

							val selectedData = props.data
							if (selectedData is Data.Simple)
								if (selectedData.id == simple)
									selected = true
						}
					}
				//TODO: handle unions

				attrs {
					id = "data-list-${props.order}"
					required = true

					onChangeFunction = { event ->
						val value = (event.target as HTMLSelectElement).value
						if (value.startsWith("simple:")) {
							val id = value.substringAfter(':').toInt()
							props.set(
								null,
								Data.simple(
									Data.Simple.SimpleDataId.values().find { it.ordinal == id }
										?: error("Aucune donnée simple n'a l'ordinal $id, c'est impossible")),
								null,
								null,
								emptyList<FormFieldComponent>().takeIf { props.recursive })
							_setSubFields(emptyList())
						} else {
							val compound = props.compounds.find { it.id == value }
								?: error("Aucune donnée n'a été trouvée avec l'identifiant '$value'")

							props.set(
								null, Data.compound(compound), null, null,
								setSubFields(compound.fields.map {
									if (it.data !is Data.Compound)
										FormFieldComponent(
											it.arity,
											it
										) to it
									else
										FormFieldComponent(
											it.arity,
											it,
											emptyList() //TODO: handle nested compound data
										) to it
								}).takeIf { props.recursive }
							)
						}
					}
				}
			}
		} else {
			when (val data = props.data) {
				is Data.Compound -> text(
					" " + (props.compounds.find { it.id == data.id }?.name
						?: error("Impossible de trouver la donnée d'identifiant '${data.id}'"))
				)
				is Data.Simple -> text(" " + data.id.displayName)
				is Data.Union -> text(" Choix entre ${data.elements.joinToString(separator = ", ")}")
			}
		}

		if (props.recursive) {
			// Subfields
			div {
				attrs {
					jsStyle {
						marginLeft = "2rem"
					}
				}

				for ((i, data) in subFields.withIndex()) {
					val (field, compound) = data
					editableField {
						this.order = i
						this.name = compound.name
						this.data = compound.data
						this.arity = field.arity
						this.compounds = props.compounds

						this.set = { name, data, min, max, subFields ->
							require(name == null) { "Il n'est pas autorisé de changer le nom d'un sous-champ" }
							require(data == null) { "Il n'est pas autorisé de changer le type d'un sous-champ" }

							val newMin = min ?: field.arity.min
							val newMax = max ?: field.arity.max
							val newSubFields = subFields ?: field.components

							props.set(
								null, null, null, null, replaceField(
									i,
									field.copy(
										arity = Arity(newMin, newMax),
										components = newSubFields
									),
									compound
								)
							)
						}

						this.allowModifications = false
						this.recursive = true
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
