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
import formulaide.ui.*
import formulaide.ui.components.*
import formulaide.ui.fields.field
import formulaide.ui.fields.immutableFields
import formulaide.ui.utils.*
import formulaide.ui.utils.DelegatedProperty.Companion.asDelegated
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.html.InputType
import kotlinx.html.js.onChangeFunction
import org.w3c.dom.HTMLInputElement
import react.*
import react.dom.br
import react.dom.div
import react.dom.p
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
		.useListEquality()
		.useEquals()
	val (searches, updateSearches) = useState(emptyList<ReviewSearch>()).asDelegated()
	var loading by useState(false)

	val allCriteria = searches.groupBy { it.action }
		.mapValues { (_, v) -> v.map { it.criterion } }

	val lambdas = useLambdas()
	val refresh: suspend () -> Unit = {
		loading = true
		val newRecords = client.todoListFor(form, state, allCriteria)

		updateRecords { newRecords }
		clearRecords()
		loading = false
	}
	val memoizedRefresh = refresh.memoIn(lambdas, "refresh", form, state, searches)

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

				refresh()
			}
		}

		cleanup {
			job.cancel()
		}
	}

	//region Full page
	div("lg:grid lg:grid-cols-3 lg:gap-y-0") {
		//region Search bar
		div("lg:order-2") {
			styledCard(
				state.displayName(),
				form.name,
				"Actualiser" to { refresh() },
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
		}
		//endregion
		//region Reviews
		div("lg:col-span-2 lg:order-1") {
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
			if (records.isEmpty()) {
				div("flex w-full justify-center items-center h-full") {
					p("my-8") { text("Aucun résultat") }
				}
			}
		}
		//endregion
	}
	//endregion
}

private external interface SearchInputProps : RProps {
	var form: Form
	var addCriterion: (ReviewSearch) -> Unit
}

private val SearchInput = memo(fc<SearchInputProps> { props ->
	val form = props.form
	val composites by useComposites()
	form.load(composites)
	var selectedRoot by useState<Action?>(null)
	val (fields, updateFields) = useState(emptyList<FormField>())
		.asDelegated()
	var criterion by useState<SearchCriterion<*>?>(null)

	val lambdas = useLambdas()

	styledField("search-field", "Rechercher dans :") {
		//region Select the root
		fun selectRoot(root: Action?) {
			selectedRoot = root
			updateFields { emptyList() }
			criterion = null
		}

		controlledSelect {
			option("Saisie originelle", "null") { selectRoot(null) }
				.selectIf { selectedRoot == null }

			for (root in form.actions.filter { it.fields != null && it.fields!!.fields.isNotEmpty() }) {
				option(root.name, root.id) { selectRoot(root) }
					.selectIf { selectedRoot == root }
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

			child(SearchInputSelect) {
				attrs {
					key = i.toString() // Safe, because the order cannot change
					this.field = fields.getOrNull(i)
					this.candidates = allCandidates
					this.allowEmpty = i != 0
					this.select = { it: FormField? ->
						updateFields {
							println("USE_EFFECT_REPLACE $it")
							if (it != null)
								subList(0, i) + it
							else
								subList(0, i)
						}
					}.memoIn(lambdas, "input-select-$i", i)
				}
			}
		}
		//endregion
	}

	val field = fields.lastOrNull()
	if (field != null) styledField("search-criterion", "Critère :") {
		//region Select the criterion type
		child(SearchCriterionSelect) {
			attrs {
				this.fields = fields
				this.select = { criterion = it }
			}
		}
		//endregion
		//region Select the criterion data
		if (criterion !is SearchCriterion.Exists && criterion != undefined) {
			styledInput(InputType.text, "search-criterion-data", required = true) {
				value = when (val c = criterion) {
					is SearchCriterion.TextContains -> c.text
					is SearchCriterion.TextEquals -> c.text
					is SearchCriterion.OrderBefore -> c.max
					is SearchCriterion.OrderAfter -> c.min
					else -> error("Aucune donnée connue pour le critère $c")
				}
				onChangeFunction = {
					val target = it.target as HTMLInputElement
					val text = target.value
					criterion = when (val c = criterion) {
						is SearchCriterion.TextContains -> c.copy(text = text)
						is SearchCriterion.TextEquals -> c.copy(text = text)
						is SearchCriterion.OrderBefore -> c.copy(max = text)
						is SearchCriterion.OrderAfter -> c.copy(min = text)
						else -> error("Aucune donnée à fournir pour le critère $c")
					}
				}
			}
		}
		//endregion
	}

	if (criterion != null) {
		styledButton("Rechercher",
		             action = {
			             props.addCriterion(ReviewSearch(
				             action = selectedRoot,
				             enabled = true,
				             criterion = criterion!!
			             ))
			             selectedRoot = null
			             updateFields { emptyList() }
			             criterion = null
		             })
	} else
		p { text("Choisissez une option pour activer la recherche.") }
})

private external interface SearchInputSelectProps : RProps {
	var field: FormField?
	var candidates: List<FormField>
	var allowEmpty: Boolean
	var select: (FormField?) -> Unit
}

private val SearchInputSelect = memo(fc<SearchInputSelectProps> { props ->
	val candidates = useMemo(props.candidates) {
		props.candidates.filter { it !is FormField.Simple || (it.simple !is SimpleField.Message && it.simple !is SimpleField.Upload) }
	}

	fun reSelect() =
		if (props.allowEmpty) null
		else candidates.firstOrNull()

	var selected by useState(
		reSelect()
	)
	useEffect(selected) { props.select(selected) }
	useEffect(candidates) { selected = reSelect() }
	useEffect(props.field) {
		if (props.field == null) {
			val newSelect = reSelect()
			selected = newSelect
			props.select(newSelect)
		}
	}

	//	text(props.field.toString())
	if (candidates.isNotEmpty()) {
		controlledSelect {
			if (props.allowEmpty)
				option("", "null") { selected = null }
					.selectIf { selected == null }

			for (candidate in candidates)
				option(candidate.name, candidate.id) { selected = candidate }
					.selectIf { selected == candidate }
		}
	}
})

private external interface SearchCriterionSelectProps : RProps {
	var fields: List<FormField>
	var select: (SearchCriterion<*>?) -> Unit
}

private val SearchCriterionSelect = fc<SearchCriterionSelectProps> { props ->
	val field = props.fields.lastOrNull()
	val fieldKey = props.fields.joinToString(separator = ":") { it.id }

	// It is CRUCIAL that these are all different from one another
	val exists = "A été rempli"
	val contains = "Contient"
	val equals = "Est exactement"
	val after = "Après"
	val before = "Avant"

	val available = ArrayList<String>().apply {
		if (field?.arity?.min == 0)
			add(exists)

		if (field is FormField.Simple || field is FormField.Union<*>)
			add(equals)

		if (field is FormField.Simple) {
			add(contains)
			add(after)
			add(before)
		}
	}

	var chosen by useState(available.firstOrNull())
	useEffect(chosen) {
		props.select(
			when (chosen) {
				exists -> SearchCriterion.Exists(fieldKey)
				contains -> SearchCriterion.TextContains(fieldKey, "")
				equals -> SearchCriterion.TextEquals(fieldKey, "")
				after -> SearchCriterion.OrderAfter(fieldKey, "")
				before -> SearchCriterion.OrderBefore(fieldKey, "")
				else -> null
			}
		)
	}
	useEffect(field) {
		chosen = available.firstOrNull()
	}

	controlledSelect {
		for (option in available)
			option(option, option) { chosen = option }
				.selectIf { chosen == option }
	}
}

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
			if (showFull) "▲"
			else "▼",
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

	var refresh: suspend () -> Unit
}

private data class ParsedTransition(
	val transition: RecordStateTransition,
	val submission: ParsedSubmission?,
)

private enum class ReviewDecision {
	PREVIOUS,
	NO_CHANGE,
	NEXT,
	REFUSE,
}

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
	val nextAction = useMemo(form.actions, actionOrNull) {
		form.actions.indexOfFirst { actionOrNull?.id == it.id }
			.takeUnless { it == -1 }
			?.let { form.actions.getOrNull(it + 1) }
			?.let { RecordState.Action(it.createRef()) }
	}
	val (selectedDestination, updateDestination) = useState(nextAction ?: state)

	if (user == null) {
		styledCard("Dossier", loading = true) { text("Chargement de l'utilisateur…") }
		return@fc
	}

	var reason by useState<String>()

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

		props.refresh()
	}

	styledFormCard(
		"Dossier",
		null,
		submit = "Confirmer" to { htmlForm ->
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
					nextState = selectedDestination,
					reason = reason,
					sendFields = true,
				)
			}
		},
		(if (showFullHistory) "Valeurs les plus récentes" else "Historique") to {
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

		val decision = when {
			selectedDestination is RecordState.Action && selectedDestination == state -> ReviewDecision.NO_CHANGE
			selectedDestination is RecordState.Refused && state is RecordState.Refused -> ReviewDecision.NO_CHANGE
			selectedDestination is RecordState.Refused -> ReviewDecision.REFUSE
			selectedDestination is RecordState.Action && selectedDestination == nextAction -> ReviewDecision.NEXT
			else -> ReviewDecision.PREVIOUS
		}

		div("mt-4") {
			text("Votre décision :")

			if ((state as? RecordState.Action)?.current?.obj != form.actions.firstOrNull())
				styledButton("Renvoyer à une étape précédente",
				             enabled = decision != ReviewDecision.PREVIOUS,
				             action = {
					             updateDestination {
						             RecordState.Action(form.actions.first().createRef())
					             }
				             })

			styledButton("Garder",
			             enabled = decision != ReviewDecision.NO_CHANGE,
			             action = { updateDestination { state } })

			if (nextAction != null)
				styledButton("Accepter",
				             enabled = decision != ReviewDecision.NEXT,
				             action = { updateDestination { nextAction } })

			if (state != RecordState.Refused)
				styledButton("Refuser",
				             enabled = decision != ReviewDecision.REFUSE,
				             action = { updateDestination { RecordState.Refused } })
		}

		if (decision == ReviewDecision.PREVIOUS)
			div {
				text("Étapes précédentes :")

				for (previousState in form.actions.map { RecordState.Action(it.createRef()) }) {
					if (previousState == state)
						break

					styledButton(previousState.current.obj.name,
					             enabled = selectedDestination != previousState,
					             action = { updateDestination { previousState } })
				}
			}

		if (decision != ReviewDecision.NEXT)
			styledField("record-${record.id}-reason", "Pourquoi ce choix ?") {
				styledInput(InputType.text,
				            "record-${record.id}-reason",
				            required = decision == ReviewDecision.REFUSE) {
					value = reason ?: ""
					onChangeFunction = {
						reason = (it.target as HTMLInputElement).value
					}
				}
			}
	}
})
