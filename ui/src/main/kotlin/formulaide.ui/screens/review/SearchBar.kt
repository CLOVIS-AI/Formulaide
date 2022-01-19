package formulaide.ui.screens.review

import formulaide.api.data.Action
import formulaide.api.data.Record
import formulaide.api.data.RecordState
import formulaide.api.search.SearchCriterion
import formulaide.client.Client
import formulaide.client.routes.downloadCsv
import formulaide.client.routes.todoListFor
import formulaide.ui.components.StyledPillContainer
import formulaide.ui.components.cards.Card
import formulaide.ui.components.cards.action
import formulaide.ui.components.useAsync
import formulaide.ui.reportExceptions
import formulaide.ui.screens.forms.list.clearRecords
import formulaide.ui.useClient
import formulaide.ui.utils.DelegatedProperty.Companion.asDelegated
import formulaide.ui.utils.onSet
import formulaide.ui.utils.remove
import formulaide.ui.utils.useEquals
import formulaide.ui.utils.useListEquality
import kotlinx.browser.document
import org.w3c.dom.url.URL
import org.w3c.files.Blob
import org.w3c.files.BlobPropertyBag
import react.FC
import react.MutableRefObject
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.p
import react.useEffect
import react.useState

external interface SearchBarProps : ReviewProps {
	var setOpenedRecords: (Map<Record, Boolean>.() -> Map<Record, Boolean>) -> Unit

	var records: List<Record>
	var updateRecords: (List<Record>.() -> List<Record>) -> Unit

	var refresh: MutableRefObject<suspend () -> Unit>
}

data class ReviewSearch(
	val action: Action?,
	val enabled: Boolean,
	val criterion: SearchCriterion<*>,
)

internal fun RecordState?.displayName() = when (this) {
	is RecordState.Action -> this.current.obj.name
	is RecordState.Refused -> "Dossiers refusés"
	null -> "Tous les dossiers"
}

/**
 * Sidebar of the [Review] screen, that allows employees to search for specific records, fold or expand all records.
 */
val SearchBar = FC<SearchBarProps>("SearchBar") { props ->
	val (client) = useClient()
	require(client is Client.Authenticated) { "Il n'est pas possible de faire une recherche sans être connecté." }

	val scope = useAsync()
	var loading by useState(false)

	suspend fun refresh(allCriteria: Map<Action?, List<SearchCriterion<*>>>) {
		loading = true
		val newRecords = client.todoListFor(props.form, props.windowState, allCriteria)

		props.updateRecords { newRecords }
		clearRecords()
		loading = false
	}

	var allCriteria by useState(emptyMap<Action?, List<SearchCriterion<*>>>())

	val (_, updateSearches) = useState(emptyList<ReviewSearch>())
		.asDelegated()
		.onSet { newSearches ->
			val newCriteria = newSearches.groupBy { it.action }
				.mapValues { (_, v) -> v.map { it.criterion } }

			allCriteria = newCriteria
			scope.reportExceptions { refresh(newCriteria) }
		}
		.useListEquality()
		.useEquals()

	useEffect(allCriteria) {
		props.refresh.current = { refresh(allCriteria) }
	}

	Card {
		title = props.windowState.displayName()
		subtitle = props.form.name
		this.loading = loading

		action("Actualiser") { refresh(allCriteria) }
		action("Exporter") {
			val file = client.downloadCsv(props.form, props.windowState, allCriteria)
			val blob = Blob(arrayOf(file), BlobPropertyBag(type = "text/csv"))

			document.createElement("a").run {
				setAttribute("href", URL.createObjectURL(blob))
				setAttribute("target", "_blank")
				setAttribute("download", "${props.form.name} - ${props.windowState.displayName()}.csv")
				setAttribute("hidden", "hidden")
				setAttribute("rel", "noopener,noreferrer")

				asDynamic().click()
				Unit
			}
		}
		action("Tout ouvrir") { props.setOpenedRecords { mapValues { true } } }
		action("Tout réduire") { props.setOpenedRecords { mapValues { false } } }

		p {
			+"${props.records.size} dossiers affichés (${Record.MAXIMUM_NUMBER_OF_RECORDS_PER_ACTION} maximum)."
		}

		div {
			SearchInput {
				+props
				addCriterion = { updateSearches { this + it } }
			}
		}

		StyledPillContainer {
			for ((root, criteria) in allCriteria) {
				for (criterion in criteria) {
					CriterionPill {
						+props

						this.root = root
						this.fields = root?.fields ?: form.mainFields
						this.criterion = criterion
						this.onRemove = {
							updateSearches {
								reportExceptions {
									val reviewSearch =
										indexOfFirst { it.action == root && it.criterion == criterion }
											.takeUnless { it == -1 }
											?: error("Impossible de trouver le critère $criterion dans la racine $root")

									remove(reviewSearch)
								}
							}
						}
					}
				}
			}
		}
	}
}
