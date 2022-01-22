package formulaide.ui.screens.review

import formulaide.api.data.Action
import formulaide.api.data.Form
import formulaide.api.fields.Field
import formulaide.api.fields.FormField
import formulaide.api.fields.FormRoot
import formulaide.api.fields.SimpleField
import formulaide.ui.components.inputs.ControlledSelect
import formulaide.ui.components.inputs.Option
import react.FC
import react.dom.html.ReactHTML.p
import react.useMemo
import react.useState

external interface SearchInputProps : SearchBarProps {
	var addCriterion: (ReviewSearch) -> Unit
}

val SearchInput = FC<SearchInputProps>("SearchInput") { props ->
	val allowedSearches = useMemo(props.form) { generateAllowedSearches(props.form).toList() }
	var selectedSearch by useState(allowedSearches.firstOrNull())

	if (selectedSearch == null) {
		p {
			className = "mt-4"
			+"Il n'est pas possible de filtrer les saisies dans ce formulaire."
		}
		return@FC
	}

	p {
		className = "mt-4"
		+"Filtrer les dossiers affichés :"
	}

	ControlledSelect {
		for (allowedSearch in allowedSearches) {
			Option(
				text = allowedSearch.name,
				value = allowedSearch.rootFieldKey,
				selected = (selectedSearch == allowedSearch),
			) { selectedSearch = allowedSearch }
		}
	}

	SelectSearchCriterionType {
		+props
		this.selectedSearch =
			selectedSearch ?: error("It should not be possible for 'selectedSearch' to be null at this point.")
	}
}

//region Generation of possible criteria

/**
 * A legal search the user could try.
 */
internal data class AllowedSearch(

	/**
	 * The [Action] this field is in, or `null` if it's an initial submission.
	 */
	val root: Action?,

	/**
	 * The path in the [root] tree used to reach this field.
	 */
	val path: List<FormField>,

	/**
	 * Whether the user can create a search from this particular node.
	 */
	val canBeSelected: Boolean = true,
) {

	/**
	 * The [FormField] this search applies to.
	 *
	 * If this [AllowedSearch] represents a [FormRoot], this [field] is `null`.
	 */
	val field get() = path.lastOrNull()

	/**
	 * The text displayed to the user when they select this search option.
	 */
	val name by lazy {
		val rootName = when (root) {
			null -> "Saisie initiale : "
			else -> "${root.name} : "
		}

		val pathNames = path.joinToString(" › ") { it.name }

		rootName + pathNames
	}

	/**
	 * Unique string that identifies the [path].
	 */
	val fieldKey by lazy {
		path.joinToString(":") { it.id }
	}

	/**
	 * Unique string that identifies the [path] and the [root].
	 */
	val rootFieldKey by lazy {
		(root?.id ?: "null") + ":" + fieldKey
	}

}

private fun generateAllowedSearches(form: Form) = sequence {
	val initial = AllowedSearch(root = null, path = emptyList(), canBeSelected = false)
	for (field in form.mainFields.fields) yieldAll(generateAllowedSearches(initial, field))

	for (action in form.actions) {
		val actionSearch = AllowedSearch(root = action, path = emptyList(), canBeSelected = false)
		for (field in action.fields?.fields ?: emptyList()) yieldAll(generateAllowedSearches(actionSearch, field))
	}
}.filter { it.canBeSelected }

private fun generateAllowedSearches(parent: AllowedSearch, field: FormField): Sequence<AllowedSearch> = sequence {
	val current = AllowedSearch(
		root = parent.root,
		path = parent.path + field,
		canBeSelected = (field is Field.Simple && field.simple != SimpleField.Message) || field is Field.Union<*>,
	)

	yield(current)

	val children = when (field) {
		is FormField.Composite -> field.fields
		is FormField.Union<*> -> field.options
		is FormField.Simple -> emptyList()
	}

	for (child in children) yieldAll(generateAllowedSearches(current, child))
}

//endregion
