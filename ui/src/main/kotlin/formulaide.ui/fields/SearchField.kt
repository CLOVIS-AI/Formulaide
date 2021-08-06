package formulaide.ui.fields

import formulaide.api.fields.FormField
import formulaide.api.fields.FormRoot
import formulaide.api.fields.SimpleField
import formulaide.api.search.SearchCriterion
import formulaide.ui.components.*
import formulaide.ui.traceRenders
import formulaide.ui.utils.text
import kotlinx.html.InputType
import kotlinx.html.js.onChangeFunction
import org.w3c.dom.HTMLInputElement
import react.RBuilder
import react.RProps
import react.child
import react.dom.attrs
import react.dom.option
import react.fc

fun RBuilder.searchFields(
	root: FormRoot,
	criteria: List<SearchCriterion<*>>,
	update: (SearchCriterion<*>?, SearchCriterion<*>?) -> Unit,
) {
	for (field in root.fields.filter { it.arity.max > 0 }.sortedBy { it.order })
		searchField(field, listOf(field.id), depth = 1, criteria = criteria, update = update)
}

private fun RBuilder.searchField(
	field: FormField,
	key: List<String>,
	depth: Int = 1,
	criteria: List<SearchCriterion<*>>,
	update: (SearchCriterion<*>?, SearchCriterion<*>?) -> Unit,
) {
	child(SearchField) {
		attrs {
			this.field = field
			this.keyList = key
			this.depth = depth
			this.criteria = criteria
			this.update = update
		}
	}
}

private external interface SearchFieldProps : RProps {
	var field: FormField
	var keyList: List<String>
	var depth: Int

	var criteria: List<SearchCriterion<*>>
	var update: (SearchCriterion<*>?, SearchCriterion<*>?) -> Unit
}

private val SearchFieldProps.fullKey: String get() = keyList.joinToString(separator = ":")
private fun SearchFieldProps.create(criterion: SearchCriterion<*>) = update(null, criterion)
private fun SearchFieldProps.remove(criterion: SearchCriterion<*>) = update(criterion, null)

private val SearchField = fc<SearchFieldProps> { props ->
	val fieldKey = props.keyList.joinToString(separator = ":")
	traceRenders("SearchField $fieldKey")
	val field = props.field

	val criteria = props.criteria.filter { it.fieldKey == fieldKey }

	val orderAfter = criteria.findInstance<SearchCriterion.OrderAfter>()
	val orderBefore = criteria.findInstance<SearchCriterion.OrderBefore>()
	val textContains = criteria.findInstance<SearchCriterion.TextContains>()
	val textEquals = criteria.findInstance<SearchCriterion.TextEquals>()
	val exists = criteria.findInstance<SearchCriterion.Exists>()

	styledNesting(depth = props.depth, field.order) {
		styledField("field-search-${field.id}", field.name) {
			if (field.arity.min == 0)
				fieldExists("Ce champ a été rempli", exists, props)

			fun subfields(fields: List<FormField>) {
				for (subField in fields.filter { it.arity.max > 0 }.sortedBy { it.order }) {
					searchField(
						subField,
						props.keyList + subField.id,
						depth = props.depth + 1,
						criteria = props.criteria,
						update = props.update
					)
				}
			}

			when (field) {
				is FormField.Simple -> {
					if (field.simple is SimpleField.Text || field.simple is SimpleField.Email || field.simple is SimpleField.Integer || field.simple is SimpleField.Decimal || field.simple is SimpleField.Date || field.simple is SimpleField.Time) {
						if (field.simple !is SimpleField.Date && field.simple !is SimpleField.Time) {
							genericCriteria(
								"Doit contenir",
								textContains,
								props,
								create = { SearchCriterion.TextContains(props.fullKey, "") },
								update = { it, value -> it.copy(text = value) }
							)
						}
						genericCriteria(
							"Doit être exactement",
							textEquals,
							props,
							inputType = if (field.simple is SimpleField.Date) InputType.date else if (field.simple is SimpleField.Time) InputType.time else InputType.text,
							create = { SearchCriterion.TextEquals(props.fullKey, "") },
							update = { it, value -> it.copy(text = value) }
						)
					}
					if (field.simple is SimpleField.Text || field.simple is SimpleField.Email || field.simple is SimpleField.Date || field.simple is SimpleField.Time) {
						genericCriteria(
							if (field.simple is SimpleField.Date || field.simple is SimpleField.Time) "Doit être après" else "Doit être après (ordre alphabétique)",
							orderAfter,
							props,
							inputType = if (field.simple is SimpleField.Date) InputType.date else if (field.simple is SimpleField.Time) InputType.time else InputType.text,
							create = { SearchCriterion.OrderAfter(props.fullKey, "") },
							update = { it, value -> it.copy(min = value) }
						)
						genericCriteria(
							if (field.simple is SimpleField.Date || field.simple is SimpleField.Time) "Doit être avant" else "Doit être avant (ordre alphabétique)",
							orderBefore,
							props,
							inputType = if (field.simple is SimpleField.Date) InputType.date else if (field.simple is SimpleField.Time) InputType.time else InputType.text,
							create = { SearchCriterion.OrderBefore(props.fullKey, "") },
							update = { it, value -> it.copy(max = value) }
						)
					}
				}
				is FormField.Union<*> -> {
					textEqualsChoice(field.options.map { it.name to it.id },
					                 "Option sélectionnée",
					                 textEquals,
					                 props)

					subfields(field.options)
				}
				is FormField.Composite -> {
					subfields(field.fields)
				}
			}
		}
	}
}

private fun <C : SearchCriterion<*>> RBuilder.genericCriteria(
	text: String,
	criterion: C?,
	props: SearchFieldProps,
	inputType: InputType = InputType.text,
	create: () -> C,
	update: (C, String) -> C,
) {
	if (criterion != null) {
		val id = idOf(criterion)
		styledField(id, "$text :") {
			styledInput(inputType, id, required = true) {
				onChangeFunction = { event ->
					val target = event.target as HTMLInputElement
					props.update(criterion, update(criterion, target.value))
				}
			}
			cancelSearchButton(criterion, props)
		}
	} else {
		enableSearchButton(text, props, create = create)
	}
}

private fun RBuilder.textEqualsChoice(
	options: List<Pair<String, String>>,
	text: String,
	criterion: SearchCriterion.TextEquals?,
	props: SearchFieldProps,
) {
	if (criterion != null) {
		val id = idOf(criterion)
		styledField(id, "$text :") {
			styledSelect(onSelect = { props.update(criterion, criterion.copy(text = it.value)) }) {
				for (option in options) {
					option {
						text(option.first)
						attrs {
							value = option.second
						}
					}
				}
			}
			cancelSearchButton(criterion, props)
		}
	} else {
		enableSearchButton(text,
		                   props,
		                   create = {
			                   SearchCriterion.TextEquals(props.fullKey,
			                                              options.first().second)
		                   })
	}
}

private fun RBuilder.fieldExists(
	text: String,
	criterion: SearchCriterion.Exists?,
	props: SearchFieldProps,
) {
	if (criterion != null) {
		val id = idOf(criterion)
		styledField(id, text) {
			cancelSearchButton(criterion, props)
		}
	} else {
		enableSearchButton(text, props, create = { SearchCriterion.Exists(props.fullKey) })
	}
}

private fun RBuilder.cancelSearchButton(criterion: SearchCriterion<*>, props: SearchFieldProps) {
	styledButton("Annuler", action = { props.remove(criterion) })
}

private fun RBuilder.enableSearchButton(
	text: String,
	props: SearchFieldProps,
	create: () -> SearchCriterion<*>,
) {
	styledButton(text, action = { props.create(create()) })
}

private fun idOf(criterion: SearchCriterion<*>) = "search-${criterion.fieldKey}-${criterion::class}"

private inline fun <reified R> Iterable<Any>.findInstance(): R? =
	find { it is R } as R?
