package formulaide.ui.fields

import formulaide.api.fields.DataField
import formulaide.api.fields.Field
import formulaide.api.fields.FormField
import formulaide.api.fields.FormField.Deep.Companion.createMatchingFormField
import formulaide.api.fields.SimpleField
import formulaide.api.types.Arity
import formulaide.api.types.Ref.Companion.loadIfNecessary
import formulaide.ui.components.styledSmallNumberInput
import formulaide.ui.utils.text
import kotlinx.html.js.onChangeFunction
import org.w3c.dom.HTMLInputElement
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
		text("Nombre de réponses autorisées : de ")
		if (field !is DataField.Composite) {
			styledSmallNumberInput("item-arity-min-${props.field.id}", required = true) {
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
		} else {
			text(arity.min.toString())

			if (arity.min != 0)
				props.replace(props.field.set(arity = Arity(0, arity.max)))
		}
		text(" à ")
		styledSmallNumberInput("item-arity-min-${props.field.id}", required = true) {
			value = arity.max.toString()
			min = arity.min.toString()
			max = "1000"
			onChangeFunction = {
				val value = (it.target as HTMLInputElement).value.toInt()
				updateSubFieldsOnMaxArityChange(props, Arity(arity.min, value))
			}
		}
	} else {
		text("Nombre de réponses autorisées : de ${arity.min} à ${arity.max} ")
	}
	when (arity) {
		Arity.mandatory() -> text(" (obligatoire)")
		Arity.optional() -> text(" (facultatif)")
		Arity.forbidden() -> text(" (interdit)")
		else -> text(" (liste)")
	}
}

private fun updateSubFieldsOnMaxArityChange(props: EditableFieldProps, newArity: Arity) {
	val newField = props.field.set(arity = newArity)

	if (newField !is FormField.Composite) {
		props.replace(newField)
	} else {
		val composite = when (newField) {
			is FormField.Shallow.Composite -> newField.ref.also { it.loadIfNecessary(props.app.composites) }.obj
			is FormField.Deep.Composite -> (newField.ref.obj as DataField.Composite).ref.also {
				it.loadIfNecessary(props.app.composites)
			}.obj
			else -> error("This is impossible, the execution should never reach this point.")
		}
		val newFields = composite.fields.map { it.createMatchingFormField(props.app.composites) }

		when (newField) {
			is FormField.Shallow.Composite -> props.replace(newField.copy(fields = newFields))
			is FormField.Deep.Composite -> props.replace(newField.copy(fields = newFields))
		}
	}
}
