package formulaide.ui.fields

import formulaide.api.data.Composite
import formulaide.api.data.SPECIAL_TOKEN_RECURSION
import formulaide.api.fields.DataField
import formulaide.api.fields.Field
import formulaide.api.fields.FormField
import formulaide.api.fields.SimpleField
import formulaide.api.types.Arity
import formulaide.api.types.Ref
import formulaide.ui.fields.SimpleFieldEnum.Companion.asEnum
import formulaide.ui.utils.text
import kotlinx.html.id
import kotlinx.html.js.onChangeFunction
import org.w3c.dom.HTMLSelectElement
import react.child
import react.dom.attrs
import react.dom.label
import react.dom.option
import react.dom.select
import react.functionalComponent

val TypeEditor = functionalComponent<EditableFieldProps> { props ->
	val field = props.field

	val allowTypeModifications = field is DataField || field is FormField.Shallow

	if (allowTypeModifications) {
		label { text("Type") }

		select {
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
				id = "field-editor-${field.id}"
				required = true

				onChangeFunction = { event ->
					val selected = (event.target as HTMLSelectElement).value

					when {
						selected.startsWith("simple:") -> {
							val newSimple = SimpleFieldEnum.fromString(selected.substringAfter(":",
							                                                                   missingDelimiterValue = ""))
								.build(Arity.mandatory())
							when (field) {
								is DataField -> props.replace(field.copyToSimple(newSimple))
								is FormField.Shallow -> props.replace(field.copyToSimple(newSimple))
							}
						}
						selected == "union" -> when (field) {
							is DataField -> props.replace(field.copyToUnion(emptyList()))
							is FormField.Shallow -> props.replace(field.copyToUnion(emptyList()))
							else -> error("Il n'est pas possible de modifier le type du champ $field")
						}
						selected.startsWith("composite:") -> {
							val selectedCompositeId =
								selected.substringAfter(":", missingDelimiterValue = "")
							val ref = Ref<Composite>(selectedCompositeId)
							if (selectedCompositeId != SPECIAL_TOKEN_RECURSION) ref.loadFrom(props.app.composites)

							when (field) {
								is DataField -> props.replace(field.copyToComposite(ref))
								is FormField.Shallow -> props.replace(
									field.copyToComposite(ref,
									                      emptyList())
										.copy(arity = Arity.forbidden()))
							}
						}
					}
				}
			}
		}
	} else {
		val typeName = when (field) {
			is Field.Union<*> -> "Choix"
			is Field.Simple -> field.simple
				.asEnum()
				.displayName
			is FormField.Deep.Composite ->
				if (!field.ref.loaded) error("Le champ n'est pas chargé, impossible d'afficher ce qu'il référence : $field")
				else (field.ref.obj as DataField.Composite).name
			else -> error("Impossible d'afficher le type du champ $field")
		}

		label { text("Type : $typeName") }
	}
}

private enum class SimpleFieldEnum(val displayName: String, val build: (Arity) -> SimpleField) {
	TEXT("Texte", { SimpleField.Text(it) }),
	INTEGER("Nombre entier", { SimpleField.Integer(it) }),
	DECIMAL("Nombre à virgule", { SimpleField.Decimal(it) }),
	BOOLEAN("Bouton à cocher", { SimpleField.Boolean(it) }),
	MESSAGE("Message", { SimpleField.Message }),
	;

	companion object {
		fun SimpleField.asEnum() = when (this) {
			is SimpleField.Text -> TEXT
			is SimpleField.Integer -> INTEGER
			is SimpleField.Decimal -> DECIMAL
			is SimpleField.Boolean -> BOOLEAN
			is SimpleField.Message -> MESSAGE
		}

		/**
		 * Finds an element of this enum from its [name].
		 * @throws NoSuchElementException if there is no such name
		 */
		fun fromString(name: String) = values().first { it.name == name }
	}
}

private val SimpleOptions = functionalComponent<EditableFieldProps> { props ->
	val field = props.field

	val current = ((field as? DataField.Simple)?.simple
		?: (field as? FormField.Shallow.Simple)?.simple)
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

private val UnionOptions = functionalComponent<EditableFieldProps> { props ->
	val field = props.field

	option {
		text("Choix")
		attrs {
			value = "union"

			selected = field is Field.Union<*>
		}
	}
}

private val CompositeOptions = functionalComponent<EditableFieldProps> { props ->
	val field = props.field

	val current = (field as? DataField.Composite)?.ref
		?: (field as? FormField.Shallow.Composite)?.ref

	for (composite in props.app.composites) {
		option {
			text(composite.name)
			attrs {
				value = "composite:${composite.id}"

				selected = current?.id == composite.id
			}
		}
	}
}

private val RecursiveCompositeOptions = functionalComponent<EditableFieldProps> { props ->
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