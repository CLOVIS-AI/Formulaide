package formulaide.ui.screens

import formulaide.api.data.*
import formulaide.api.types.Ref.Companion.createRef
import formulaide.api.types.Ref.Companion.load
import formulaide.client.Client
import formulaide.client.routes.findSubmission
import formulaide.client.routes.review
import formulaide.client.routes.todoListFor
import formulaide.ui.components.*
import formulaide.ui.fields.field
import formulaide.ui.reportExceptions
import formulaide.ui.traceRenders
import formulaide.ui.useClient
import formulaide.ui.useUser
import formulaide.ui.utils.parseHtmlForm
import formulaide.ui.utils.text
import react.*
import react.dom.p
import kotlin.js.Date

internal fun RecordState.displayName() = when (this) {
	is RecordState.Action -> this.current.obj.id //TODO: replace by the name after #109
	is RecordState.Done -> "Acceptés"
	is RecordState.Refused -> "Refusés"
}

@Suppress("FunctionName")
internal fun Review(form: Form, state: RecordState, initialRecords: List<Record>) = fc<RProps> {
	traceRenders("Review ${form.name}")

	val (client) = useClient()

	var records by useState(initialRecords)

	if (client !is Client.Authenticated) {
		styledCard("Vérification des dossiers", failed = true) {
			text("Seuls les employés peuvent accéder à cette page.")
		}
		return@fc
	}

	val refresh: suspend () -> Unit = { records = client.todoListFor(form, state) }

	styledCard(
		"Liste des dossiers",
		"${form.name}, dossiers ${state.displayName()}",
		"Actualiser" to refresh,
	) {
		p { text("${records.size} dossiers sont chargés. Pour des raisons de performance, il n'est pas possible de charger plus de ${Record.MAXIMUM_NUMBER_OF_RECORDS_PER_ACTION} dossiers à la fois.") }
	}

	for (record in records) {
		child(ReviewRecord) {
			attrs {
				this.form = form
				this.record = record

				this.refresh = refresh
			}
		}
	}

}

private external interface ReviewRecordProps : RProps {
	var form: Form
	var record: Record

	var refresh: suspend () -> Unit
}

private val ReviewRecord = fc<ReviewRecordProps> { props ->
	val form = props.form
	val record = props.record
	val scope = useAsync()
	val (client) = useClient()
	val (user) = useUser()

	record.form.load(form)
	require(client is Client.Authenticated) { "Seuls les employés peuvent accéder à cette page" }

	val (_, setRefreshSubmission) = useState(0)
	useEffect(record) {
		for (submission in record.submissions)
			scope.reportExceptions {
				submission.load { client.findSubmission(it) }
				setRefreshSubmission.invoke { it + 1 }
			}
	}

	val state = record.state
	val actionOrNull = (state as? RecordState.Action)?.current
	val nextState = form.actions.indexOfFirst { actionOrNull?.id == it.id }
		.takeUnless { it == -1 }
		?.let { form.actions.getOrNull(it + 1) }
		?.let { RecordState.Action(it.createRef()) }
		?: RecordState.Done

	val submitButtonText =
		if (state == RecordState.Done || state == RecordState.Refused) "Enregistrer"
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
						nextState,
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
	) {
		var i = 0

		for (submission in record.submissions) {
			styledNesting(depth = 0, fieldNumber = i) {
				if (submission.loaded) {
					p { text(submission.obj.data.toString()) }
				} else {
					p { text("Chargement…"); loadingSpinner() }
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
