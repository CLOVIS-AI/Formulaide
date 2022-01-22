package formulaide.ui.screens.review

import formulaide.api.fields.FormField
import formulaide.api.fields.SimpleField
import formulaide.api.search.SearchCriterion
import formulaide.ui.components.inputs.Input
import react.FC
import react.dom.html.InputType

internal external interface SelectSearchCriterionMetadataProps : SelectSearchCriterionTypeProps {
	var criterion: SearchCriterion<*>
	var updateCriterion: (SearchCriterion<*>?.() -> SearchCriterion<*>?) -> Unit
}

/**
 * For [search criteria][SearchCriterion] that have some metadata, lets the user select its value.
 */
internal val SelectSearchCriterionMetadata =
	FC<SelectSearchCriterionMetadataProps>("SelectSearchCriterionMetadata") { props ->
		val field = props.selectedSearch.field
		val criterion = props.criterion

		if (criterion !is SearchCriterion.Exists) {
			val inputType = when {
				field is FormField.Simple && field.simple is SimpleField.Date -> InputType.date
				field is FormField.Simple && field.simple is SimpleField.Time -> InputType.time
				field is FormField.Simple && field.simple is SimpleField.Email -> InputType.email
				else -> InputType.text
			}

			Input {
				type = inputType
				id = "search-criterion-data"
				required = true

				value = when (criterion) {
					is SearchCriterion.TextContains -> criterion.text
					is SearchCriterion.TextEquals -> criterion.text
					is SearchCriterion.OrderBefore -> criterion.max
					is SearchCriterion.OrderAfter -> criterion.min
					else -> error("Aucune donnée connue pour le critère $criterion")
				}

				onChange = {
					val target = it.target
					val text = target.value
					props.updateCriterion {
						when (criterion) {
							is SearchCriterion.TextContains -> criterion.copy(text = text)
							is SearchCriterion.TextEquals -> criterion.copy(text = text)
							is SearchCriterion.OrderBefore -> criterion.copy(max = text)
							is SearchCriterion.OrderAfter -> criterion.copy(min = text)
							else -> error("Aucune donnée à fournir pour le critère $criterion")
						}
					}
				}
			}
		}
	}
