package formulaide.ui.screens

import formulaide.api.data.*
import formulaide.api.search.SearchCriterion
import formulaide.api.types.Ref.Companion.createRef
import formulaide.api.types.Ref.Companion.load
import formulaide.client.Client
import formulaide.client.routes.compositesReferencedIn
import formulaide.client.routes.findSubmission
import formulaide.client.routes.review
import formulaide.client.routes.todoListFor
import formulaide.ui.components.*
import formulaide.ui.fields.field
import formulaide.ui.fields.immutableFields
import formulaide.ui.fields.searchFields
import formulaide.ui.reportExceptions
import formulaide.ui.traceRenders
import formulaide.ui.useClient
import formulaide.ui.useUser
import formulaide.ui.utils.parseHtmlForm
import formulaide.ui.utils.replace
import formulaide.ui.utils.text
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import react.*
import react.dom.br
import react.dom.p
import kotlin.js.Date

internal fun RecordState.displayName() = when (this) {
	is RecordState.Action -> this.current.obj.name
	is RecordState.Refused -> "Dossiers refusés"
}

private data class ReviewSearch(
	val action: Action?,
	val enabled: Boolean,
	val criteria: List<SearchCriterion<*>>,
)

@Suppress("FunctionName")
internal fun Review(form: Form, state: RecordState, initialRecords: List<Record>) = fc<RProps> {
	traceRenders("Review ${form.name}")

	val (client) = useClient()
	require(client is Client.Authenticated) { "Seuls les employés peuvent accéder à cette page." }

	var records by useState(initialRecords)
	var searches by useState(
		listOf(ReviewSearch(null, false, emptyList())) +
				form.actions
					.filter { it.fields != null }
					.map { ReviewSearch(it, false, emptyList()) }
	)
	var loading by useState(false)

	val allCriteria = searches.flatMap { it.criteria }
	val refresh: suspend () -> Unit = {
		loading = true
		val newRecords = client.todoListFor(form, state, allCriteria)

		val retained = records.filter { it in newRecords }
		records = retained + newRecords.filter { record -> record.id !in retained.map { it.id } }
		loading = false
	}

	useEffect(searches) {
		val job = Job()

		CoroutineScope(job).launch {
			reportExceptions {
				delay(300)

				refresh()
			}
		}

		cleanup {
			job.cancel()
		}
	}

	styledCard(
		state.displayName(),
		form.name,
		"Actualiser" to refresh,
		loading = loading,
	) {
		p { text("${records.size} dossiers sont chargés. Pour des raisons de performance, il n'est pas possible de charger plus de ${Record.MAXIMUM_NUMBER_OF_RECORDS_PER_ACTION} dossiers à la fois.") }

		for ((i, search) in searches.withIndex()) {
			fun updateSearch(
				enabled: Boolean = search.enabled,
				criteria: List<SearchCriterion<*>> = search.criteria,
			) {
				searches = searches.replace(i, search.copy(enabled = enabled, criteria = criteria))
			}

			styledNesting(depth = 0, fieldNumber = i) {
				if (search.enabled) {
					searchFields(
						search.action?.fields ?: form.mainFields,
						criteria = search.criteria,
						update = { previous, next ->
							if (previous == null) {
								updateSearch(criteria = search.criteria + next!!)
							} else if (next == null) {
								updateSearch(criteria = search.criteria - previous)
							} else {
								updateSearch(criteria = search.criteria.replace(i, next))
							}
						}
					)

					styledButton("Annuler la recherche",
					             action = {
						             searches = searches.replace(i, search.copy(enabled = false))
					             })
				} else {
					val message = "Recherche : " +
							(if (search.action == null) "champs originaux" else "étape ${search.action.id}") //TODO replace with action name

					styledButton(
						message,
						action = { updateSearch(enabled = true) }
					)
				}
			}
		}
	}

	for (record in records) {
		child(ReviewRecord) {
			attrs {
				this.form = form
				this.record = record

				this.refresh = refresh

				key = record.id
			}
		}
	}

}

private external interface ReviewRecordProps : RProps {
	var form: Form
	var record: Record

	var refresh: suspend () -> Unit
}

private data class ParsedTransition(
	val transition: RecordStateTransition,
	val submission: ParsedSubmission?,
)

private val ReviewRecord = fc<ReviewRecordProps> { props ->
	traceRenders("ReviewRecord ${props.record.id}")
	val form = props.form
	val record = props.record
	val scope = useAsync()
	val (client) = useClient()
	val (user) = useUser()

	record.form.load(form)
	record.load()
	require(client is Client.Authenticated) { "Seuls les employés peuvent accéder à cette page" }

	var showFullHistory by useState(false)
	val (fullHistory, setFullHistory) = useState(record.history.map { ParsedTransition(it, null) })
	val history =
		if (showFullHistory) fullHistory
		else fullHistory.groupBy { it.transition.previousState }
			.mapNotNull { (_, v) -> v.maxByOrNull { it.transition.timestamp } }

	useEffect(fullHistory, showFullHistory) {
		for ((i, parsed) in history.withIndex()) {
			val fields = parsed.transition.fields
			if (fields != null && parsed.submission == null) {
				scope.launch {
					fields.load { client.findSubmission(it) }
					val newParsed = parsed.copy(submission = fields.obj.parse(form))
					setFullHistory { full -> full.replace(i, newParsed) }
				}
			}
		}
	}

	var formLoaded by useState(false)
	useEffect(form) {
		scope.reportExceptions {
			form.load(client.compositesReferencedIn(form))
			formLoaded = true
		}
	}

	val state = record.state
	val actionOrNull = (state as? RecordState.Action)?.current
	val nextState = form.actions.indexOfFirst { actionOrNull?.id == it.id }
		.takeUnless { it == -1 }
		?.let { form.actions.getOrNull(it + 1) }
		?.let { RecordState.Action(it.createRef()) }

	val submitButtonText =
		if (state == RecordState.Refused) "Enregistrer"
		else "Accepter"

	if (user == null) {
		styledCard("Dossier", loading = true) { text("Chargement de l'utilisateur…") }
		return@fc
	}

	styledFormCard(
		"Dossier",
		null,
		submit = submitButtonText to { htmlForm ->
			val submission = if (state is RecordState.Action)
				parseHtmlForm(
					htmlForm,
					form,
					state.current.obj,
				)
			else null

			launch {
				client.review(ReviewRequest(
					record.createRef(),
					RecordStateTransition(
						(Date.now() / 1000).toLong(),
						state,
						nextState ?: state,
						assignee = user.createRef(),
						reason = null,
					),
					submission,
				))

				props.refresh()
			}
		},
		"Refuser" to {
			client.review(ReviewRequest(
				record.createRef(),
				RecordStateTransition(
					(Date.now() / 1000).toLong(),
					state,
					RecordState.Refused,
					assignee = user.createRef(),
					reason = "NOT YET IMPLEMENTED", //TODO add a reason to the review process
				),
				fields = null
			))

			props.refresh()
		},
		(if (showFullHistory) "Valeurs les plus récentes" else "Historique complet") to {
			showFullHistory = !showFullHistory
		}
	) {
		traceRenders("ReviewRecord card")
		var i = 0

		for (parsed in history) {
			styledNesting(depth = 0, fieldNumber = i) {
				val transition = parsed.transition
				styledTitle(transition.previousState?.displayName() ?: "Saisie originelle")
				p { text("Déplacé vers ${transition.nextState.displayName()} à ${transition.timestamp}.") }
				if (transition.previousState != null) {
					p {
						text("Déplacé par ${transition.assignee?.id}")
						if (transition.reason != null)
							text(" parce que \"${transition.reason}\"")
						text(".")
					}
				}

				if (transition.fields != null) {
					br {}
					if (parsed.submission == null || !formLoaded) {
						p { text("Chargement des saisies…"); loadingSpinner() }
					} else {
						immutableFields(parsed.submission)
					}
				}
			}
			i++
		}

		if (state is RecordState.Action) {
			styledNesting(depth = 0, fieldNumber = i) {
				state.current.loadFrom(form.actions, lazy = true)
				val action = state.current.obj

				val root = action.fields
				if (root != null) {
					for (field in root.fields) {
						field(field)
					}
				}
				i++
			}
		}
	}
}
