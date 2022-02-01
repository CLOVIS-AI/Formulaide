package formulaide.ui.screens.review

import formulaide.api.data.Action
import formulaide.api.fields.FormField
import formulaide.api.fields.FormRoot
import formulaide.api.search.SearchCriterion
import formulaide.ui.components.StyledButton
import formulaide.ui.components.StyledPill
import react.FC
import react.dom.html.ReactHTML.br
import react.useMemo
import react.useState

external interface CriterionPillProps : SearchBarProps {
	var root: Action?
	var fields: FormRoot

	var criterion: SearchCriterion<*>
	var onRemove: suspend () -> Unit
}

/**
 * Small pill used to display a specific [search criterion][SearchCriterion].
 *
 * These are displayed in the [Review] page, in the [sidebar][SearchBar].
 */
val CriterionPill = FC<CriterionPillProps>("CriterionPill") { props ->
	var expanded by useState(false)

	/**
	 * From the [SearchCriterion.fieldKey], find all the [FormField] instances from the root of the form to the selected field.
	 */
	val fields = useMemo(props.fields, props.criterion.fieldKey) {
		val fieldKeys = props.criterion.fieldKey.split(":")
		val fields = ArrayList<FormField>(fieldKeys.size)

		fields.add(props.fields.fields.find { it.id == fieldKeys[0] } ?: error("Aucun champ n'a l'ID ${fieldKeys[0]}"))

		for ((i, key) in fieldKeys.withIndex().drop(1)) {
			fields.add(when (val current = fields[i - 1]) {
				           is FormField.Simple -> error("Impossible d'obtenir un enfant d'un champ simple")
				           is FormField.Union<*> -> current.options.find { it.id == key }
					           ?: error("Aucune option n'a l'ID $key: ${current.options}")
				           is FormField.Composite -> current.fields.find { it.id == key }
					           ?: error("Aucun champ n'a l'ID $key: ${current.fields}")
			           })
		}

		fields
	}

	StyledPill {
		StyledButton {
			text = if (expanded) "▲" else "▼"
			action = { expanded = !expanded }
		}

		if (expanded) {
			+(props.root?.name ?: "Saisie initiale")

			for (field in fields) {
				br {}
				+"→ ${field.name}"
			}
		} else {
			+fields.last().name
		}

		+" "
		+when (val criterion = props.criterion) {
			is SearchCriterion.Exists -> "a été rempli"
			is SearchCriterion.TextContains -> "contient « ${criterion.text} »"
			is SearchCriterion.TextEquals -> "est exactement « ${criterion.text} »"
			is SearchCriterion.OrderBefore -> "est avant ${criterion.max}"
			is SearchCriterion.OrderAfter -> "est après ${criterion.min}"
		}

		StyledButton {
			text = "×"
			action = { props.onRemove() }
		}
	}
}
