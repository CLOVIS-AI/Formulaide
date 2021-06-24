package formulaide.ui.fields

import formulaide.api.data.*
import formulaide.api.data.Data.Simple.SimpleDataId.*
import formulaide.api.types.Arity
import formulaide.ui.ScreenProps
import formulaide.ui.utils.text
import kotlinx.html.INPUT
import kotlinx.html.InputType
import react.*
import react.dom.*

private external interface FieldProps : RProps {
	var app: ScreenProps

	var id: String

	var name: String
	var arity: Arity
	var type: Data

	var formField: AbstractFormField
	var compoundField: CompoundDataField?
}

private val Field: FunctionalComponent<FieldProps> = functionalComponent { props ->
	br {}
	label {
		text(props.name)

		if (props.arity == Arity.mandatory())
			text("*")
	}

	val configureName: RDOMBuilder<INPUT>.() -> Unit = {
		attrs {
			println("Configuring field with name ${props.id}")
			name = props.id
		}
	}

	val configureRequired: RDOMBuilder<INPUT>.() -> Unit = {
		attrs {
			required = props.arity == Arity.mandatory()
		}
	}

	when (val type = props.type) {
		is Data.Simple -> when (type.id) { //TODO: check validity of value
			TEXT -> input(InputType.text) {
				configureName()
				configureRequired()
			}
			INTEGER -> input(InputType.number) {
				configureName()
				configureRequired()
			}
			MESSAGE -> Unit // Nothing to do, see MESSAGE
			DECIMAL -> input(InputType.number) {
				configureName()
				configureRequired()
				attrs {
					step = "any"
				}
			}
			BOOLEAN -> input(InputType.checkBox) {
				configureName()
			}
		}
		is Data.Compound -> {
			val compound = props.app.compounds.find { it.id == type.id }
			requireNotNull(compound) { "L'identifiant '${type.id}' ne correspond à aucune donnée connue" }

			div {
				attrs {
					jsStyle {
						marginLeft = "2rem"
					}
				}

				for (formField in props.formField.components!!) {
					val compoundField = compound.fields.find { it.id == formField.id }
					requireNotNull(compoundField) { "Le champ du formulaire ${formField.id} ne correspond à aucun champ de la donnée '${compound.id}'" }

					deepField(props.app, formField, compoundField, props.id)
				}
			}
		}
		is Data.Union -> Unit //TODO: unions
	}

	//TODO: lists
}

fun RBuilder.topLevelField(
	screenProps: ScreenProps,
	field: FormField,
	parentId: String?,
) = child(Field) {
	attrs {
		app = screenProps

		id = (parentId?.plus(":") ?: "") + field.id.toString()

		name = field.name
		arity = field.arity
		type = field.data

		formField = field
		compoundField = null
	}
}

fun RBuilder.deepField(
	screenProps: ScreenProps,
	field: FormFieldComponent,
	compoundField: CompoundDataField,
	parentId: String?
) = child(Field) {
	attrs {
		app = screenProps

		id = (parentId?.plus(":") ?: "") + field.id.toString()

		name = compoundField.name
		arity = field.arity
		type = compoundField.data

		formField = field
		this.compoundField = compoundField
	}
}
