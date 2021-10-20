package formulaide.ui.fields

import formulaide.api.data.Composite
import formulaide.api.fields.*
import formulaide.api.fields.DeepFormField.Companion.createMatchingFormField
import formulaide.api.types.*
import formulaide.api.types.Ref.Companion.loadIfNecessary
import formulaide.ui.components.styledButton
import formulaide.ui.components.styledDisabledButton
import formulaide.ui.components.styledSmallInput
import formulaide.ui.useComposites
import formulaide.ui.utils.text
import kotlinx.html.InputType
import kotlinx.html.js.onChangeFunction
import org.w3c.dom.HTMLInputElement
import react.fc
import kotlin.math.max
import kotlin.math.min

val ArityEditor = fc<EditableFieldProps>("ArityEditor") { props ->
	val field = props.field
	val arity = field.arity

	val composites by useComposites()

	//region Allowed
	// Editing the arity is allowed for all types but Message
	val simpleOrNull = (field as? Field.Simple)?.simple
	val allowModifications =
		if (simpleOrNull != null) simpleOrNull::class != SimpleField.Message::class
		else true
	//endregion
	//region Allowed range

	val minAllowedRange = when (field) {
		is DataField.Composite -> 0..0 // composite references can never be mandatory (see DataField.Composite)
		is DeepFormField -> field.dataField.arity.min..Int.MAX_VALUE
		else -> 0..Int.MAX_VALUE
	}

	val maxAllowedRange = when (field) {
		is ShallowFormField.Composite -> 0..Int.MAX_VALUE
		is DataField, is ShallowFormField -> 1..Int.MAX_VALUE
		is DeepFormField -> 0..field.dataField.arity.max
		else -> 0..Int.MAX_VALUE
	}

	//endregion

	if (allowModifications) {
		val modelArities = mapOf(
			Arity.forbidden() to "Caché",
			Arity.optional() to "Facultatif",
			Arity.mandatory() to "Obligatoire",
		).filterKeys { it.min in minAllowedRange && it.max in maxAllowedRange }

		for ((modelArity, arityName) in modelArities) {
			if (modelArity == arity) {
				styledDisabledButton(arityName)
			} else {
				styledButton(arityName) {
					updateSubFieldsOnMaxArityChange(props, modelArity, composites)
				}
			}
		}

		if (arity.max > 1) {
			text("De ")
			if (field !is DataField.Composite) {
				styledSmallInput(InputType.number,
				                 "item-arity-min-${props.uniqueId}",
				                 required = true) {
					value = arity.min.toString()
					min = minAllowedRange.first.toString()
					max = min(arity.max, minAllowedRange.last).toString()
					onChangeFunction = {
						val value = (it.target as HTMLInputElement).value.toInt()
						props.replace(
							props.field.requestCopy(arity = Arity(value, arity.max))
						)
					}
				}
			} else {
				text(arity.min.toString())
			}
			text(" à ")
			styledSmallInput(InputType.number,
			                 "item-arity-min-${props.uniqueId}",
			                 required = true) {
				value = arity.max.toString()
				min = max(arity.min, max(maxAllowedRange.first, 2)).toString()
				max = maxAllowedRange.last.toString()
				onChangeFunction = {
					val value = (it.target as HTMLInputElement).value.toInt()
					updateSubFieldsOnMaxArityChange(props, Arity(arity.min, value), composites)
				}
			}
			text(" réponses")
		} else if (maxAllowedRange.last > 1) {
			styledButton("Plusieurs réponses") {
				props.replace(props.field.requestCopy(
					arity = Arity.list(0, 5)
						.expandMin(minAllowedRange.last)
						.truncateMin(minAllowedRange.first)
						.expandMax(maxAllowedRange.first)
						.truncateMax(maxAllowedRange.last)
				))
			}
		}
	} else {
		when (arity) {
			Arity.mandatory() -> styledDisabledButton("Obligatoire")
			Arity.optional() -> styledDisabledButton("Facultatif")
			Arity.forbidden() -> styledDisabledButton("Caché")
			else -> styledDisabledButton("De ${arity.min} à ${arity.max} réponses")
		}
	}
}

private fun updateSubFieldsOnMaxArityChange(
	props: EditableFieldProps,
	newArity: Arity,
	composites: List<Composite>,
) {
	val newField = props.field.requestCopy(arity = newArity)

	if (newField !is FormField.Composite) {
		props.replace(newField)
	} else {
		val composite = when (newField) {
			is ShallowFormField.Composite -> newField.ref
				.also { it.loadIfNecessary(composites) }
				.obj
			is DeepFormField.Composite -> (newField.ref.obj as DataField.Composite).ref
				.also { it.loadIfNecessary(composites) }
				.obj
			else -> error("This is impossible, the execution should never reach this point.")
		}
		val newFields = composite.fields.map { it.createMatchingFormField(composites) }

		when (newField) {
			is ShallowFormField.Composite -> props.replace(newField.copy(fields = newFields))
			is DeepFormField.Composite -> props.replace(newField.copy(fields = newFields))
		}
	}
}
