package formulaide.ui.fields

import formulaide.api.data.Composite
import formulaide.api.data.SPECIAL_TOKEN_RECURSION
import formulaide.api.fields.*
import formulaide.api.types.Arity
import formulaide.api.types.Ref
import formulaide.ui.components.styledField
import formulaide.ui.components.styledFormField
import formulaide.ui.components.styledLightText
import formulaide.ui.components.styledSelect
import formulaide.ui.fields.SimpleFieldEnum.Companion.asEnum
import formulaide.ui.useComposites
import formulaide.ui.utils.text
import kotlinx.html.id
import react.child
import react.dom.attrs
import react.dom.option
import react.fc

val TypeEditor = fc<EditableFieldProps> { props ->
	val composites by useComposites()

	val field = props.field

	val allowTypeModifications = field is DataField || field is ShallowFormField

	if (allowTypeModifications) {
		val typeId = "item-type-${props.uniqueId}"

		styledField(typeId, "Type") {
			styledSelect(
				onSelect = { onSelect(it.value, field, props, composites) }
			) {
				child(SimpleOptions) {
					attrs { inheritFrom(props) }
				}

				child(UnionOptions) {
					attrs { inheritFrom(props) }
				}

				child(CompositeOptions) {
					attrs { inheritFrom(props) }
				}

				// Select itself
				child(RecursiveCompositeOptions) {
					attrs { inheritFrom(props) }
				}

				attrs {
					id = typeId
					required = true
				}
			}
		}
	} else {
		val typeName = when (field) {
			is Field.Union<*> -> "Choix"
			is Field.Simple -> field.simple
				.asEnum()
				.displayName
			is DeepFormField.Composite ->
				if (!field.ref.loaded) error("Le champ n'est pas chargé, impossible d'afficher ce qu'il référence : $field")
				else (field.ref.obj as DataField.Composite).name
			else -> error("Impossible d'afficher le type du champ $field")
		}

		styledFormField { text(field.name + " "); styledLightText(typeName) }
	}
}

private fun onSelect(
	selected: String,
	field: Field,
	props: EditableFieldProps,
	composites: List<Composite>,
) {
	when {
		selected.startsWith("simple:") -> {
			val newSimple =
				SimpleFieldEnum.fromString(selected.substringAfter(":",
				                                                   missingDelimiterValue = ""))
					.build(Arity.mandatory())
			when (field) {
				is DataField -> props.replace(field.copyToSimple(newSimple))
				is ShallowFormField -> props.replace(field.copyToSimple(
					newSimple))
			}
		}
		selected == "union" -> when (field) {
			is DataField -> props.replace(field.copyToUnion(emptyList()))
			is ShallowFormField -> props.replace(field.copyToUnion(emptyList()))
			else -> error("Il n'est pas possible de modifier le type du champ $field")
		}
		selected.startsWith("composite:") -> {
			val selectedCompositeId =
				selected.substringAfter(":", missingDelimiterValue = "")
			val ref = Ref<Composite>(selectedCompositeId)
			if (selectedCompositeId != SPECIAL_TOKEN_RECURSION) ref.loadFrom(composites)

			when (field) {
				is DataField -> props.replace(field.copyToComposite(ref))
				is ShallowFormField -> props.replace(
					field.copyToComposite(ref,
					                      emptyList())
						.copy(arity = Arity.forbidden()))
			}
		}
	}
}

private enum class SimpleFieldEnum(val displayName: String, val build: (Arity) -> SimpleField) {
	TEXT("Texte", { SimpleField.Text(it) }),
	MESSAGE("Texte non modifiable", { SimpleField.Message }),
	INTEGER("Nombre entier", { SimpleField.Integer(it) }),
	DECIMAL("Nombre à virgule", { SimpleField.Decimal(it) }),
	BOOLEAN("Bouton à cocher", { SimpleField.Boolean(it) }),
	EMAIL("Adresse mail", { SimpleField.Email(it) }),
	PHONE("Numéro de téléphone", { SimpleField.Phone(it) }),
	DATE("Date", { SimpleField.Date(it) }),
	TIME("Heure", { SimpleField.Time(it) }),
	UPLOAD("Fichier", { SimpleField.Upload(it, allowedFormats = emptyList()) }),
	;

	companion object {
		fun SimpleField.asEnum() = when (this) {
			is SimpleField.Text -> TEXT
			is SimpleField.Integer -> INTEGER
			is SimpleField.Decimal -> DECIMAL
			is SimpleField.Boolean -> BOOLEAN
			is SimpleField.Message -> MESSAGE
			is SimpleField.Email -> EMAIL
			is SimpleField.Phone -> PHONE
			is SimpleField.Date -> DATE
			is SimpleField.Time -> TIME
			is SimpleField.Upload -> UPLOAD
		}

		/**
		 * Finds an element of this enum from its [name].
		 * @throws NoSuchElementException if there is no such name
		 */
		fun fromString(name: String) = values().first { it.name == name }
	}
}

private val SimpleOptions = fc<EditableFieldProps> { props ->
	val field = props.field

	val current = ((field as? DataField.Simple)?.simple
		?: (field as? ShallowFormField.Simple)?.simple)
		?.asEnum()

	for (simple in SimpleFieldEnum.values()) {
		option {
			text(simple.displayName)
			attrs {
				value = "simple:$simple"

				selected = simple == current
			}
		}
	}
}

private val UnionOptions = fc<EditableFieldProps> { props ->
	val field = props.field

	option {
		text("Choix")
		attrs {
			value = "union"

			selected = field is Field.Union<*>
		}
	}
}

private val CompositeOptions = fc<EditableFieldProps> { props ->
	val composites by useComposites()

	val field = props.field

	val current = (field as? DataField.Composite)?.ref
		?: (field as? ShallowFormField.Composite)?.ref

	for (composite in composites) {
		option {
			text(composite.name)
			attrs {
				value = "composite:${composite.id}"

				selected = current?.id == composite.id
			}
		}
	}
}

private val RecursiveCompositeOptions = fc<EditableFieldProps> { props ->
	val field = props.field

	// Only data can be recursive, forms cannot
	val allow = field is DataField

	if (allow) {
		val current = (field as? DataField.Composite)?.ref

		option {
			text("La donnée qui est en train de se faire créer")
			attrs {
				value = "composite:$SPECIAL_TOKEN_RECURSION"

				selected = current?.id == SPECIAL_TOKEN_RECURSION
			}
		}
	}
}
