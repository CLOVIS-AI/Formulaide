package formulaide.ui.screens

import formulaide.api.data.*
import formulaide.api.fields.FormField
import formulaide.api.fields.FormRoot
import formulaide.api.fields.SimpleField
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
import formulaide.ui.reportExceptions
import formulaide.ui.traceRenders
import formulaide.ui.useClient
import formulaide.ui.useUser
import formulaide.ui.utils.DelegatedProperty.Companion.asDelegated
import formulaide.ui.utils.parseHtmlForm
import formulaide.ui.utils.remove
import formulaide.ui.utils.replace
import formulaide.ui.utils.text
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.html.InputType
import kotlinx.html.js.onChangeFunction
import org.w3c.dom.HTMLInputElement
import react.*
import react.dom.*
import kotlin.js.Date

internal fun RecordState.displayName() = when (this) {
	is RecordState.Action -> this.current.obj.name
	is RecordState.Refused -> "Dossiers refusés"
}

private data class ReviewSearch(
	val action: Action?,
	val enabled: Boolean,
	val criterion: SearchCriterion<*>,
)

@Suppress("FunctionName")
internal fun Review(form: Form, state: RecordState, initialRecords: List<Record>) = fc<RProps> {
	traceRenders("Review ${form.name}")

	val scope = useAsync()
	val (client) = useClient()
	require(client is Client.Authenticated) { "Seuls les employés peuvent accéder à cette page." }

	val (records, updateRecords) = useState(initialRecords).asDelegated()
	val (searches, updateSearches) = useState(emptyList<ReviewSearch>()).asDelegated()
	var loading by useState(false)

	val allCriteria = searches.groupBy { it.action }
		.mapValues { (_, v) -> v.map { it.criterion } }

	val lambdas = useLambdas()
	val refresh: suspend (Boolean) -> Unit = { forceUpdate ->
		loading = true
		val newRecords = client.todoListFor(form, state, allCriteria)

		updateRecords {
			if (forceUpdate) {
				traceRenders("Search Force Update")
				newRecords
			} else {
				traceRenders("Search Nice Update")
				val retained = filter { it in newRecords }
				retained +
						newRecords.filter { record -> record.id !in retained.map { it.id } }
			}
		}
		loading = false
	}
	val memoizedRefresh: suspend (Boolean) -> Unit =
		refresh.memoIn(lambdas, "refresh", form, state, searches)

	var formLoaded by useState(false)
	useEffect(form) {
		scope.reportExceptions {
			form.load(client.compositesReferencedIn(form))
			formLoaded = true
		}
	}

	useEffect(searches) {
		val job = Job()

		CoroutineScope(job).launch {
			reportExceptions {
				delay(300)

				refresh(false)
			}
		}

		cleanup {
			job.cancel()
		}
	}

	styledCard(
		state.displayName(),
		form.name,
		"Actualiser" to { refresh(true) },
		loading = loading,
	) {
		p { text("${records.size} dossiers sont chargés. Pour des raisons de performance, il n'est pas possible de charger plus de ${Record.MAXIMUM_NUMBER_OF_RECORDS_PER_ACTION} dossiers à la fois.") }

		div {
			child(SearchInput) {
				attrs {
					this.form = form
					this.addCriterion = { updateSearches { this + it } }
				}
			}
		}

		styledPillContainer {
			for ((root, criteria) in allCriteria)
				for (criterion in criteria)
					child(CriterionPill) {
						attrs {
							this.root = root
							this.fields = root?.fields ?: form.mainFields
							this.criterion = criterion
							this.onRemove = {
								updateSearches {
									reportExceptions {
										val reviewSearch =
											indexOfFirst { it.action == root && it.criterion == criterion }
												.takeUnless { it == -1 }
												?: error("Impossible de trouver le critère $criterion dans la racine $root, ce n'est pas possible !")

										remove(reviewSearch)
									}
								}
							}.memoIn(lambdas, "pill-$criterion", criterion, root, searches)
						}
					}
		}
	}

	for (record in records) {
		child(ReviewRecord) {
			attrs {
				this.form = form
				this.formLoaded = formLoaded
				this.record = record

				this.refresh = memoizedRefresh

				key = record.id
			}
		}
	}

}

private external interface SearchInputProps : RProps {
	var form: Form
	var addCriterion: (ReviewSearch) -> Unit
}

private val SearchInput = memo(fc<SearchInputProps> { props ->
	val form = props.form
	var selectedRoot by useState<Action?>(null)
	var fields by useState(emptyList<FormField>())
	var criterion by useState<SearchCriterion<*>>()

	styledField("search-field", "Rechercher dans :") {
		//region Select the root
		styledSelect(
			onSelect = { select ->
				selectedRoot =
					if (select.value == "null") null
					else form.actions.find { it.id == select.value }
				criterion = null
			}
		) {
			option {
				text("Saisie originelle")
				attrs {
					value = "null"
					selected = selectedRoot == null
				}
			}

			for (root in form.actions.filter { it.fields != null && it.fields!!.fields.isNotEmpty() }) {
				option {
					text(root.name)
					attrs {
						value = root.id
						selected = selectedRoot == root
					}
				}
			}
		}
		//endregion
		//region Select the current field
		for (i in 0..fields.size) { // fields.size+1 loops on purpose
			val allCandidates: List<FormField> =
				if (i == 0)
					if (selectedRoot == null) form.mainFields.fields
					else selectedRoot!!.fields!!.fields
				else when (val lastParent = fields[i - 1]) {
					is FormField.Simple -> emptyList()
					is FormField.Union<*> -> lastParent.options
					is FormField.Composite -> lastParent.fields
				}
			val candidates = allCandidates
				.filter { it !is FormField.Simple || it.simple !is SimpleField.Message }

			if (candidates.isNotEmpty()) {
				styledSelect(
					onSelect = { select ->
						val candidate = candidates.find { it.id == select.value }

						fields = if (candidate != null)
							fields.subList(0, i) + candidate
						else
							fields.subList(0, i)
					}
				) {
					if (i != 0)
						option {
							text("")
							attrs {
								value = "null"
							}
						}

					for (candidate in candidates) {
						option {
							text(candidate.name)
							attrs {
								value = candidate.id
							}
						}
					}
				}
			}
		}
		//endregion
	}

	div {
		//TODO: select the criterion

		//TODO: criterion parameters
	}

	if (criterion != null)
		styledButton("Rechercher",
		             action = {
			             props.addCriterion(ReviewSearch(
				             action = selectedRoot,
				             enabled = true,
				             criterion = criterion!!
			             ))
		             })
	else
		p { text("Choisissez une option pour activer la recherche.") }
})

private external interface CriterionPillProps : RProps {
	var root: Action?
	var fields: FormRoot
	var criterion: SearchCriterion<*>
	var onRemove: () -> Unit
}

private val CriterionPill = memo(fc<CriterionPillProps> { props ->
	var showFull by useState(false)

	val fields = useMemo(props.fields, props.criterion.fieldKey) {
		val fieldKeys = props.criterion.fieldKey.split(":")
		val fields = ArrayList<FormField>(fieldKeys.size)

		fields.add(props.fields.fields.find { it.id == fieldKeys[0] }
			           ?: error("Aucun champ n'a l'ID ${fieldKeys[0]}"))

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

	styledPill {
		styledButton(
			if (showFull) "‹"
			else "›",
			action = { showFull = !showFull }
		)
		if (showFull) {
			text(props.root?.name ?: "Saisie originelle")

			for (field in fields) {
				br {}
				text("→ ${field.name}")
			}
		} else {
			text(fields.last().name)
		}
		text(" ")
		text(when (val criterion = props.criterion) {
			     is SearchCriterion.Exists -> "a été rempli"
			     is SearchCriterion.TextContains -> "contient « ${criterion.text} »"
			     is SearchCriterion.TextEquals -> "est exactement « ${criterion.text} »"
			     is SearchCriterion.OrderBefore -> "est avant ${criterion.max}"
			     is SearchCriterion.OrderAfter -> "est après ${criterion.min}"
		     })
		styledButton("×", action = { props.onRemove() })
	}
})

private external interface ReviewRecordProps : RProps {
	var form: Form
	var record: Record

	var formLoaded: Boolean

	var refresh: suspend (Boolean) -> Unit
}

private data class ParsedTransition(
	val transition: RecordStateTransition,
	val submission: ParsedSubmission?,
)

private val ReviewRecord = memo(fc<ReviewRecordProps> { props ->
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
	fun getHistory() = record.history.map { ParsedTransition(it, null) }
	val (fullHistory, setFullHistory) = useState(getHistory())
	val history =
		if (showFullHistory) fullHistory
			.sortedBy { it.transition.timestamp }
		else fullHistory.groupBy { it.transition.previousState }
			.mapNotNull { (_, v) -> v.maxByOrNull { it.transition.timestamp } }
			.sortedBy { it.transition.timestamp }

	useEffect(record) {
		val newHistory = getHistory()
		if (newHistory.map { it.transition } != fullHistory.map { it.transition }) {
			setFullHistory(newHistory)
		}
	}

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

	val state = record.state
	val actionOrNull = (state as? RecordState.Action)?.current
	val nextState = form.actions.indexOfFirst { actionOrNull?.id == it.id }
		.takeUnless { it == -1 }
		?.let { form.actions.getOrNull(it + 1) }
		?.let { RecordState.Action(it.createRef()) }
	var selectedNextState by useState(nextState ?: state)

	if (user == null) {
		styledCard("Dossier", loading = true) { text("Chargement de l'utilisateur…") }
		return@fc
	}

	var reason by useState<String>()
	var warnMandatoryReason by useState(false)

	suspend fun review(
		fields: FormSubmission?,
		nextState: RecordState?,
		reason: String?,
		sendFields: Boolean = true,
	) {
		client.review(ReviewRequest(
			record.createRef(),
			RecordStateTransition(
				(Date.now() / 1000).toLong(),
				state,
				nextState ?: state,
				assignee = user.createRef(),
				reason = reason,
			),
			fields.takeIf { sendFields },
		))

		props.refresh(true)
	}

	styledFormCard(
		"Dossier",
		null,
		submit = "Enregistrer" to { htmlForm ->
			val submission =
				if (state is RecordState.Action && state.current.obj.fields?.fields?.isNotEmpty() == true)
					parseHtmlForm(
						htmlForm,
						form,
						state.current.obj,
					)
				else null

			launch {
				review(
					submission,
					nextState = selectedNextState,
					reason = reason,
					sendFields = true,
				)
			}
		},
		"Refuser" to {
			if (reason.isNullOrBlank()) {
				warnMandatoryReason = true
			} else {
				review(
					fields = null,
					nextState = RecordState.Refused,
					reason = reason,
					sendFields = false,
				)
			}
		},
		(if (showFullHistory) "Afficher uniquement les valeurs les plus récentes" else "Historique complet") to {
			showFullHistory = !showFullHistory
		}
	) {
		var i = 0

		for (parsed in history) {
			styledNesting(depth = 0, fieldNumber = i) {
				val transition = parsed.transition
				val title = transition.previousState?.displayName() ?: "Saisie originelle"
				if (showFullHistory) {
					styledTitle("$title → ${transition.nextState.displayName()}")
				} else {
					styledTitle(title)
				}
				val timestamp = Date(transition.timestamp * 1000)
				if (transition.previousState != null) {
					p {
						text("Par ${transition.assignee?.id}")
						if (transition.reason != null)
							text(" parce que \"${transition.reason}\"")
						text(", le ${timestamp.toLocaleString()}.")
					}
				} else {
					p { text("Le ${timestamp.toLocaleString()}.") }
				}

				if (transition.fields != null) {
					if (parsed.submission == null || !props.formLoaded) {
						p { text("Chargement des saisies…"); loadingSpinner() }
					} else {
						br {}
						immutableFields(parsed.submission)
					}
				}
			}
			i++
		}

		if (state is RecordState.Action) {
			state.current.loadFrom(form.actions, lazy = true)
			val action = state.current.obj

			val root = action.fields
			if (root != null) {
				styledNesting(depth = 0, fieldNumber = i) {
					for (field in root.fields) {
						field(form, action, field)
					}
					i++
				}
			}
		}

		div {
			text("Envoyer ce dossier à :")

			for (candidateNextState in form.actions.map { RecordState.Action(it.createRef()) }) {
				if (selectedNextState != candidateNextState) {
					styledButton(
						candidateNextState.current.obj.name,
						action = { selectedNextState = candidateNextState },
					)
				} else {
					styledDisabledButton(candidateNextState.current.obj.name)
				}

				if (candidateNextState == nextState)
					break
			}

			if (state == RecordState.Refused) {
				if (selectedNextState != RecordState.Refused) {
					styledButton(RecordState.Refused.displayName(),
					             action = { selectedNextState = RecordState.Refused })
				} else {
					styledDisabledButton(RecordState.Refused.displayName())
				}
			}
		}

		styledField("record-${record.id}-reason", "Pourquoi ce choix ?") {
			styledInput(InputType.text, "record-${record.id}-reason") {
				value = reason ?: ""
				onChangeFunction = {
					reason = (it.target as HTMLInputElement).value
				}
			}

			if (warnMandatoryReason)
				styledErrorText("Ce champ est obligatoire pour un refus.")
		}
	}
})
