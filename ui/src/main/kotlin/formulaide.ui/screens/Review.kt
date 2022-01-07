package formulaide.ui.screens

import formulaide.api.data.*
import formulaide.api.fields.*
import formulaide.api.search.SearchCriterion
import formulaide.api.types.Ref.Companion.createRef
import formulaide.api.types.Ref.Companion.load
import formulaide.client.Client
import formulaide.client.routes.*
import formulaide.ui.components.*
import formulaide.ui.components.cards.Card
import formulaide.ui.components.cards.FormCard
import formulaide.ui.components.cards.action
import formulaide.ui.components.cards.submit
import formulaide.ui.components.inputs.Nesting
import formulaide.ui.components.text.Text
import formulaide.ui.components.text.Title
import formulaide.ui.fields.field
import formulaide.ui.fields.immutableFields
import formulaide.ui.reportExceptions
import formulaide.ui.traceRenders
import formulaide.ui.useClient
import formulaide.ui.useUser
import formulaide.ui.utils.*
import formulaide.ui.utils.DelegatedProperty.Companion.asDelegated
import kotlinx.browser.document
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.w3c.dom.url.URL
import org.w3c.files.Blob
import org.w3c.files.BlobPropertyBag
import react.*
import react.dom.html.InputType
import react.dom.html.ReactHTML.br
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.p
import react.dom.html.ReactHTML.table
import react.dom.html.ReactHTML.tbody
import react.dom.html.ReactHTML.td
import react.dom.html.ReactHTML.th
import react.dom.html.ReactHTML.thead
import react.dom.html.ReactHTML.tr
import kotlin.js.Date
import formulaide.ui.components.inputs.Field as UIField
import formulaide.ui.components.inputs.Input as UIInput

internal fun RecordState?.displayName() = when (this) {
	is RecordState.Action -> this.current.obj.name
	is RecordState.Refused -> "Dossiers refusés"
	null -> "Tous les dossiers"
}

private data class ReviewSearch(
	val action: Action?,
	val enabled: Boolean,
	val criterion: SearchCriterion<*>,
)

@Suppress("FunctionName")
internal fun Review(form: Form, state: RecordState?, initialRecords: List<Record>) = FC<Props>("Review") {
	traceRenders("Review ${form.name}")

	val scope = useAsync()
	val (client) = useClient()
	require(client is Client.Authenticated) { "Seuls les employés peuvent accéder à cette page." }

	val (records, updateRecords) = useState(initialRecords).asDelegated().useListEquality().useEquals()
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

	val (openedRecords, setOpenedRecords) = useState(records.associateWith { true })
		.asDelegated()

	var composites by useState(emptyList<Composite>())
	var formLoaded by useState(false)
	useEffect(form) {
		scope.reportExceptions {
			val referenced = client.compositesReferencedIn(form)
			form.load(referenced)
			composites = referenced
			formLoaded = true
		}
	}

	useEffect(searches) {
		val job = Job()

		CoroutineScope(job).launch {
			reportExceptions {
				refresh()
			}
		}

		cleanup {
			job.cancel()
		}
	}

	val columnsToDisplay = useMemo(form.mainFields) {
		form.mainFields.asSequenceWithKey()
			.filter { (_, it) -> it !is FormField.Composite }
			.filter { (_, it) -> it !is FormField.Simple || it.simple != SimpleField.Message }
			.toList()
	}

	//region Full page
	div {
		traceRenders("Review … page")
		className = "lg:grid lg:grid-cols-3 lg:gap-y-0"

		//region Search bar
		div {
			traceRenders("Review … search bar")
			className = "lg:order-2"

			Card {
				title = state.displayName()
				subtitle = form.name

				action("Actualiser", refresh)
				action("Exporter") {
					val file = client.downloadCsv(form, state, allCriteria)
					val blob = Blob(arrayOf(file), BlobPropertyBag(type = "text/csv"))

					document.createElement("a").run {
						setAttribute("href", URL.createObjectURL(blob))
						setAttribute("target", "_blank")
						setAttribute("download", "${form.name} - ${state.displayName()}.csv")
						setAttribute("hidden", "hidden")
						setAttribute("rel", "noopener,noreferrer")

						asDynamic().click()
						Unit
					}
				}
				action("Tout ouvrir") { setOpenedRecords { mapValues { true } } }
				action("Tout réduire") { setOpenedRecords { mapValues { false } } }

				this.loading = loading

				p {
					Text {
						text =
							"${records.size} dossiers sont chargés. Pour des raisons de performance, il n'est pas possible de charger plus de ${Record.MAXIMUM_NUMBER_OF_RECORDS_PER_ACTION} dossiers à la fois."
					}
				}

				div {
					SearchInput {
						this.form = form
						this.formLoaded = formLoaded
						this.addCriterion = { updateSearches { this + it } }
					}
				}

				StyledPillContainer {
					for ((root, criteria) in allCriteria) for (criterion in criteria) CriterionPill {
						this.root = root
						this.fields = root?.fields ?: form.mainFields
						this.criterion = criterion
						this.onRemove = {
							updateSearches {
								reportExceptions {
									val reviewSearch =
										indexOfFirst { it.action == root && it.criterion == criterion }.takeUnless { it == -1 }
											?: error("Impossible de trouver le critère $criterion dans la racine $root, ce n'est pas possible !")

									remove(reviewSearch)
								}
							}
						}.memoIn(lambdas, "pill-$criterion", criterion, root, searches)
					}
				}
			}
		}
		//endregion
		//region Reviews
		div {
			traceRenders("Review … main contents")
			className = "lg:col-span-2 lg:order-1 w-full overflow-x-auto"

			table {
				className = "table-auto w-full"

				thead {
					tr {
						val thClasses = "first:pl-8 last:pr-8 py-2"

						if (state == null) th {
							className = thClasses
							div {
								className = "mx-4"
								Text { text = "Étape" }
							}
						}

						if (formLoaded) {
							columnsToDisplay.forEach { (_, it) ->
								th {
									className = thClasses
									div {
										className = "mx-4"
										Text { text = it.name }
									}
								}
							}
						}
					}
				}

				tbody {
					for (record in records) {
						ReviewRecord {
							this.form = form
							this.windowState = state
							this.composites = composites
							this.formLoaded = formLoaded
							this.record = record

							this.refresh = memoizedRefresh
							this.columnsToDisplay = columnsToDisplay

							this.collapsed = !(openedRecords[record] ?: true)
							this.collapse = { it: Boolean -> setOpenedRecords { this + (record to !it) } }.memoIn(
								lambdas,
								"record-${record.id}-collapse")
							key = record.id
						}
					}
					if (records.isEmpty()) {
						td {
							colSpan = form.mainFields.asSequence().count() + (if (state == null) 1 else 0)
							div {
								className = "flex justify-center items-center w-full h-full"
								p {
									className = "my-8"
									Text { text = "Aucun résultat" }
								}
							}
						}
					}
				}
			}
		}
		//endregion
		traceRenders("Review … done")
	}
	//endregion
}

private external interface SearchInputProps : Props {
	var form: Form
	var formLoaded: Boolean
	var addCriterion: (ReviewSearch) -> Unit
}

private val SearchInput = memo(FC<SearchInputProps>("SearchInput") { props ->
	val form = props.form
	var selectedRoot by useState<Action?>(null)
	val (fields, updateFields) = useState(emptyList<FormField>()).asDelegated()
	var criterion by useState<SearchCriterion<*>?>(null)

	val lambdas = useLambdas()

	if (!props.formLoaded) {
		Text { text = "Chargement du formulaire en cours…" }
		LoadingSpinner()
		return@FC
	}

	UIField {
		id = "search-field"
		text = "Rechercher dans :"

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
			val allCandidates: List<FormField> = if (i == 0) if (selectedRoot == null) form.mainFields.fields
			else selectedRoot!!.fields!!.fields
			else when (val lastParent = fields[i - 1]) {
				is FormField.Simple -> emptyList()
				is FormField.Union<*> -> lastParent.options
				is FormField.Composite -> lastParent.fields
			}

			SearchInputSelect {
				key = i.toString() // Safe, because the order cannot change
				this.field = fields.getOrNull(i)
				this.candidates = allCandidates
				this.allowEmpty = i != 0
				this.select = { it: FormField? ->
					updateFields {
						if (it != null) subList(0, i) + it
						else subList(0, i)
					}
				}.memoIn(lambdas, "input-select-$i", i)
			}
		}
		//endregion
	}

	val field = fields.lastOrNull()
	if (field != null) UIField {
		id = "search-criterion"
		text = "Critère :"

		//region Select the criterion type
		SearchCriterionSelect {
			this.fields = fields
			this.select = { criterion = it }
		}
		//endregion

		//region Select the criterion data
		if (criterion !is SearchCriterion.Exists && criterion != undefined) {
			val inputType = when {
				field is FormField.Simple && field.simple is SimpleField.Date -> InputType.date
				field is FormField.Simple && field.simple is SimpleField.Time -> InputType.time
				field is FormField.Simple && field.simple is SimpleField.Email -> InputType.email
				else -> InputType.text
			}

			UIInput {
				type = inputType
				id = "search-criterion-data"
				required = true

				value = when (val c = criterion) {
					is SearchCriterion.TextContains -> c.text
					is SearchCriterion.TextEquals -> c.text
					is SearchCriterion.OrderBefore -> c.max
					is SearchCriterion.OrderAfter -> c.min
					else -> error("Aucune donnée connue pour le critère $c")
				}

				onChange = {
					val target = it.target
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
		StyledButton {
			text = "Rechercher"
			action = {
				props.addCriterion(ReviewSearch(
					action = selectedRoot,
					enabled = true,
					criterion = criterion!!
				))
				selectedRoot = null
				updateFields { emptyList() }
				criterion = null
			}
		}
	} else
		p { Text { text = "Choisissez une option pour activer la recherche." } }
})

private external interface SearchInputSelectProps : Props {
	var field: FormField?
	var candidates: List<FormField>
	var allowEmpty: Boolean
	var select: (FormField?) -> Unit
}

private val SearchInputSelect = memo(FC<SearchInputSelectProps>("SearchInputSelect") { props ->
	val candidates = useMemo(props.candidates) {
		props.candidates.filter { it !is FormField.Simple || (it.simple !is SimpleField.Message && it.simple !is SimpleField.Upload) }
	}

	fun reSelect() = if (props.allowEmpty) null
	else candidates.firstOrNull()

	var selected by useState(reSelect()
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

private external interface SearchCriterionSelectProps : Props {
	var fields: List<FormField>
	var select: (SearchCriterion<*>?) -> Unit
}

private val SearchCriterionSelect = FC<SearchCriterionSelectProps>("SearchCriterionSelect") { props ->
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

		if (field is FormField.Simple && field.simple !is SimpleField.Boolean) {
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

private external interface CriterionPillProps : Props {
	var root: Action?
	var fields: FormRoot
	var criterion: SearchCriterion<*>
	var onRemove: () -> Unit
}

private val CriterionPill = memo(FC<CriterionPillProps>("CriterionPill") { props ->
	var showFull by useState(false)

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
			text = if (showFull) "▲" else "▼"
			action = { showFull = !showFull }
		}
		if (showFull) {
			Text { text = props.root?.name ?: "Saisie originelle" }

			for (field in fields) {
				br {}
				Text { text = "→ ${field.name}" }
			}
		} else {
			Text { text = fields.last().name }
		}
		Text { text = " " }
		Text {
			text = when (val criterion = props.criterion) {
				is SearchCriterion.Exists -> "a été rempli"
				is SearchCriterion.TextContains -> "contient « ${criterion.text} »"
				is SearchCriterion.TextEquals -> "est exactement « ${criterion.text} »"
				is SearchCriterion.OrderBefore -> "est avant ${criterion.max}"
				is SearchCriterion.OrderAfter -> "est après ${criterion.min}"
			}
		}
		StyledButton {
			text = "×"
			action = { props.onRemove() }
		}
	}
})

private external interface ReviewRecordProps : Props {
	var form: Form
	var windowState: RecordState?
	var composites: List<Composite>
	var record: Record

	var collapsed: Boolean
	var collapse: (Boolean) -> Unit

	var formLoaded: Boolean
	var columnsToDisplay: List<Pair<String, FormField>>

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

private val ReviewRecord = memo(FC<ReviewRecordProps>("ReviewRecord") { props ->
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
		reportExceptions {
			val newHistory = getHistory()
			if (newHistory.map { it.transition } != fullHistory.map { it.transition }) {
				setFullHistory(newHistory)
			}
		}
	}

	useEffect(fullHistory, showFullHistory) {
		traceRenders("ReviewRecord … history calculation and loading")
		reportExceptions {
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
	}

	traceRenders("ReviewRecord … Next action")
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
		traceRenders("ReviewRecord … cancelled")
		Card {
			title = "Dossier"
			loading = true
			Text { text = "Chargement de l'utilisateur…" }
		}
		return@FC
	}

	var reason by useState<String>()

	val collapsed = props.collapsed

	if (collapsed) tr {
		ReviewRecordCollapsed {
			this.collapse = props.collapse
			this.history = history
			this.state = state
			this.windowState = props.windowState
			this.columnsToDisplay = props.columnsToDisplay
		}
	} else tr {
		ReviewRecordExpanded {
			this.form = form
			this.formLoaded = props.formLoaded
			this.composites = props.composites
			this.windowState = props.windowState
			this.state = state
			this.record = props.record
			this.refresh = props.refresh
			this.collapse = props.collapse
			this.history = history
			this.showFullHistory = showFullHistory
			this.updateShowFullHistory = { showFullHistory = it }
			this.nextAction = nextAction
			this.selectedDestination = selectedDestination
			this.updateDestination = updateDestination
			this.reason = reason
			this.updateReason = { reason = it }
		}
	}

	traceRenders("ReviewRecord … done")
})

//region ReviewRecord Collapsed

private external interface ReviewRecordCollapsedProps : Props {
	var windowState: RecordState?
	var state: RecordState
	var history: List<ParsedTransition>
	var columnsToDisplay: List<Pair<String, FormField>>
	var collapse: (Boolean) -> Unit
}

private val ReviewRecordCollapsed = FC<ReviewRecordCollapsedProps>("ReviewRecordCollapsed") { props ->
	traceRenders("ReviewRecordCollapsed")
	val tdClasses = "first:pl-8 last:pr-8 py-2"
	val tdDivClasses = "mx-4"

	if (props.windowState == null) td {
		className = tdClasses

		div {
			className = tdDivClasses
			Text { text = props.state.displayName() }
		}
	}

	val parsed = props.history.first { it.transition.previousState == null }
	requireNotNull(parsed.submission) { "Une saisie initiale est nécessairement déjà remplie" }

	for ((key, _) in props.columnsToDisplay) {
		fun parsedAsSequence(p: ParsedField<*>): Sequence<ParsedField<*>> = sequence {
			yield(p)
			p.children?.let { child -> yieldAll(child.flatMap { parsedAsSequence(it) }) }
		}

		val parsedField = parsed.submission.fields
			.asSequence()
			.flatMap { parsedAsSequence(it) }
			.firstOrNull { it.fullKeyString == key }

		td {
			className = tdClasses
			div {
				className = tdDivClasses

				if (parsedField is ParsedList<*>) {
					Text {
						text = parsedField.children.mapNotNull { it.rawValue }
							.joinToString(separator = ", ")
					}
				} else {
					Text { text = parsedField?.rawValue ?: "" }
				}
			}
		}
	}

	td {
		StyledButton {
			text = "▼"
			action = { props.collapse(false) }
		}
		traceRenders("ReviewRecordCollapsed … done")
	}
}

//endregion
//region ReviewRecord Expanded

private external interface ReviewRecordExpandedProps : Props {
	var form: Form
	var formLoaded: Boolean
	var composites: List<Composite>
	var windowState: RecordState?
	var state: RecordState
	var record: Record
	var refresh: suspend () -> Unit

	var collapse: (Boolean) -> Unit

	var history: List<ParsedTransition>
	var showFullHistory: Boolean
	var updateShowFullHistory: (Boolean) -> Unit
	var nextAction: RecordState.Action?

	var selectedDestination: RecordState
	var updateDestination: StateSetter<RecordState>
	var reason: String?
	var updateReason: (String?) -> Unit
}

private val ReviewRecordExpanded = FC<ReviewRecordExpandedProps>("ReviewRecordExpanded") { props ->
	traceRenders("ReviewRecordExpanded")

	val state = props.state
	val (client) = useClient()
	require(client is Client.Authenticated) { "Seuls les employés peuvent accéder à cette page" }

	suspend fun review(
		fields: FormSubmission?,
		nextState: RecordState?,
		reason: String?,
		sendFields: Boolean = true,
	) {
		client.review(ReviewRequest(
			props.record.createRef(),
			RecordStateTransition(
				(Date.now() / 1000).toLong(),
				state,
				nextState ?: state,
				assignee = client.me.createRef(),
				reason = reason,
			),
			fields.takeIf { sendFields },
		))

		props.refresh()
	}

	td {
		colSpan = props.form.mainFields.asSequence().count() +
				(if (props.windowState == null) 1 else 0)

		if (props.windowState != null) FormCard {
			title = "Dossier"

			submit("Confirmer") { htmlForm ->
				val submission =
					if (state is RecordState.Action && state.current.obj.fields?.fields?.isNotEmpty() == true)
						parseHtmlForm(
							htmlForm,
							props.form,
							state.current.obj,
						)
					else null

				launch {
					review(
						submission,
						nextState = props.selectedDestination,
						reason = props.reason,
						sendFields = true,
					)
				}
			}
			action("Réduire") { props.collapse(true) }
			action(if (props.showFullHistory) "Valeurs les plus récentes" else "Historique") {
				props.updateShowFullHistory(!props.showFullHistory)
			}

			traceRenders("ReviewRecordExpanded … card with decisions")
			ReviewRecordContents {
				this.form = props.form
				this.record = props.record
				this.state = state
				this.formLoaded = props.formLoaded
				this.showFullHistory = props.showFullHistory
				this.history = props.history
				this.composites = props.composites
				this.windowState = props.windowState
			}

			traceRenders("ReviewRecordExpanded … decision area")
			val selectedDestination = props.selectedDestination
			val decision = when {
				selectedDestination is RecordState.Action && selectedDestination == state -> ReviewDecision.NO_CHANGE
				selectedDestination is RecordState.Refused && state is RecordState.Refused -> ReviewDecision.NO_CHANGE
				selectedDestination is RecordState.Refused -> ReviewDecision.REFUSE
				selectedDestination is RecordState.Action && selectedDestination == props.nextAction -> ReviewDecision.NEXT
				else -> ReviewDecision.PREVIOUS
			}

			div {
				className = "mt-4"
				Text { text = "Votre décision :" }

				traceRenders("ReviewRecordExpanded … Previous action")
				if ((state as? RecordState.Action)?.current?.obj != props.form.actions.firstOrNull())
					StyledButton {
						text = "Renvoyer à une étape précédente"
						enabled = decision != ReviewDecision.PREVIOUS
						action = {
							props.updateDestination {
								RecordState.Action(props.form.actions.first().createRef())
							}
						}
					}

				traceRenders("ReviewRecordExpanded … Keep")
				StyledButton {
					text = "Conserver"
					enabled = decision != ReviewDecision.NO_CHANGE
					action = { props.updateDestination { state }; props.updateReason(null) }
				}

				traceRenders("ReviewRecordExpanded … Accept")
				val nextAction = props.nextAction
				if (nextAction != null)
					StyledButton {
						text = "Accepter"
						enabled = decision != ReviewDecision.NEXT
						action = { props.updateDestination { nextAction } }
					}

				traceRenders("ReviewRecordExpanded … Refuse")
				if (state != RecordState.Refused)
					StyledButton {
						text = "Refuser"
						enabled = decision != ReviewDecision.REFUSE
						action = { props.updateDestination { RecordState.Refused } }
					}
			}

			if (decision == ReviewDecision.PREVIOUS)
				div {
					traceRenders("ReviewRecordExpanded … Display previous actions")
					Text { text = "Étapes précédentes :" }

					for (previousState in props.form.actions.map { RecordState.Action(it.createRef()) }) {
						if (previousState == state)
							break

						StyledButton {
							text = previousState.current.obj.name
							enabled = selectedDestination != previousState
							action = { props.updateDestination { previousState } }
						}
					}
				}

			traceRenders("ReviewRecordExpanded … Reason")
			if (decision != ReviewDecision.NEXT)
				UIField {
					id = "record-${props.record.id}-reason"
					text = "Pourquoi ce choix ?"

					UIInput {
						type = InputType.text
						id = "record-${props.record.id}-reason"
						required = decision == ReviewDecision.REFUSE
						value = props.reason ?: ""
						onChange = { props.updateReason(it.target.value) }
					}
				}
			traceRenders("ReviewRecordExpanded … end of card")
		} else Card {
			title = "Dossiers"
			subtitle = state.displayName()
			action("Réduire") { props.collapse(true) }

			traceRenders("ReviewRecordExpanded … card without decisions")
			ReviewRecordContents {
				this.form = props.form
				this.record = props.record
				this.state = state
				this.formLoaded = props.formLoaded
				this.showFullHistory = props.showFullHistory
				this.history = props.history
				this.composites = props.composites
				this.windowState = props.windowState
			}
		}
	}
	traceRenders("ReviewRecordExpanded … done")
}

//endregion
//region ReviewRecord Card

private external interface ReviewRecordContentsProps : Props {
	var history: List<ParsedTransition>
	var showFullHistory: Boolean
	var formLoaded: Boolean

	var form: Form
	var state: RecordState
	var windowState: RecordState?
	var record: Record

	var composites: List<Composite>
}

private val ReviewRecordContents = FC<ReviewRecordContentsProps>("ReviewRecordContents") { props ->
	traceRenders("ReviewRecordCard")
	var i = 0

	var loaded by useState(false)
	useEffect(props.composites, props.formLoaded) {
		val state = props.state
		if (state is RecordState.Action) reportExceptions {
			state.current.loadFrom(props.form.actions, lazy = true)
			val action = state.current.obj

			if (props.formLoaded) { // if the form isn't loaded, props.composites is empty
				action.fields?.load(props.composites, allowNotFound = !props.formLoaded)
				loaded = true
			}
		} else {
			loaded = true
		}
	}
	if (!props.formLoaded) {
		traceRenders("ReviewRecordCard … cancelled because the form is not loaded")
		Text { text = "Chargement du formulaide…" }
		LoadingSpinner()
		return@FC
	}
	if (!loaded) {
		traceRenders("ReviewRecordCard … cancelled because the action fields are not loaded")
		Text { text = "Chargement des champs…" }
		LoadingSpinner()
		return@FC
	}

	for (parsed in props.history) {
		Nesting {
			depth = 0
			fieldNumber = i

			val transition = parsed.transition
			val title = transition.previousState?.displayName() ?: "Saisie originelle"
			if (props.showFullHistory) {
				Title { this.title = "$title → ${transition.nextState.displayName()}" }
			} else {
				Title { this.title = title }
			}
			val timestamp = Date(transition.timestamp * 1000)
			if (transition.previousState != null) {
				p {
					Text { text = "Par ${transition.assignee?.id}" }
					if (transition.reason != null)
						Text { text = " parce que \"${transition.reason}\"" }
					Text { text = ", le ${timestamp.toLocaleString()}." }
				}
			} else {
				p { Text { text = "Le ${timestamp.toLocaleString()}." } }
			}

			if (transition.fields != null) {
				if (!props.formLoaded) {
					p { Text { text = "Chargement du formulaire…" }; LoadingSpinner() }
				} else if (parsed.submission == null) {
					p { Text { text = "Chargement de la saisie…" }; LoadingSpinner() }
				} else {
					br {}
					immutableFields(parsed.submission)
				}
			}
		}

		i++
	}

	val state = props.state
	if (state is RecordState.Action && props.windowState != null) {
		val action = state.current.obj

		val root = action.fields
		if (root != null) {
			Nesting {
				depth = 0
				fieldNumber = i

				for (field in root.fields) {
					field(props.form, action, field, key = "${props.record.id}_${field.id}")
				}
				i++
			}
		}
	}

	traceRenders("ReviewRecordCard … done")
}

//endregion
