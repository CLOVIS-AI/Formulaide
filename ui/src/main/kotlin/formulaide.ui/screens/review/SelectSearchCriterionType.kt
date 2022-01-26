package formulaide.ui.screens.review

import formulaide.api.fields.FormField
import formulaide.api.fields.SimpleField
import formulaide.api.search.SearchCriterion
import formulaide.ui.components.StyledButton
import formulaide.ui.components.inputs.ControlledSelect
import formulaide.ui.components.inputs.Option
import formulaide.ui.utils.DelegatedProperty.Companion.asDelegated
import react.FC
import react.useEffect
import react.useMemo
import react.useState

internal external interface SelectSearchCriterionTypeProps : SearchInputProps {
	var selectedSearch: AllowedSearch
}

/**
 * Lets the user choose between one of the criterion types available for their [selectedSearch][SelectSearchCriterionTypeProps.selectedSearch].
 */
internal val SelectSearchCriterionType = FC<SelectSearchCriterionTypeProps>("SelectSearchCriterionType") { props ->
	val field =
		props.selectedSearch.field ?: error("The selected field should never be 'null': ${props.selectedSearch}")
	val fieldKey = props.selectedSearch.fieldKey

	// These values SHOULD be different from each other
	val exists = "A été rempli"
	val contains = "Contient"
	val equals = "Est exactement"
	val after = "Après"
	val before = "Avant"

	val available = useMemo(field) {
		ArrayList<String>().apply {
			if (field.arity.min == 0)
				add(exists)

			if (field is FormField.Simple || field is FormField.Union<*>)
				add(equals)

			if (field is FormField.Simple && field.simple !is SimpleField.Boolean) {
				add(contains)
				add(after)
				add(before)
			}
		}
	}

	var chosen by useState(available.firstOrNull())
	val (criterion, updateCriterion) = useState<SearchCriterion<*>>()
		.asDelegated()

	useEffect(available) {
		chosen = available.firstOrNull()
	}

	useEffect(chosen) {
		updateCriterion {
			when (chosen) {
				exists -> SearchCriterion.Exists(fieldKey)
				contains -> SearchCriterion.TextContains(fieldKey, "")
				equals -> SearchCriterion.TextEquals(fieldKey, "")
				after -> SearchCriterion.OrderAfter(fieldKey, "")
				before -> SearchCriterion.OrderBefore(fieldKey, "")
				else -> error("Could not recognize")
			}
		}
	}

	ControlledSelect {
		for (option in available)
			Option(option, option, selected = chosen == option) { chosen = option }
	}

	if (criterion != null) {
		SelectSearchCriterionMetadata {
			+props
			this.criterion = criterion
			this.updateCriterion = updateCriterion
		}

		StyledButton {
			text = "Rechercher"
			action = {
				props.addCriterion(
					ReviewSearch(
						action = props.selectedSearch.root,
						enabled = true,
						criterion = criterion
					)
				)
				chosen = available.firstOrNull()
			}
		}
	}
}
