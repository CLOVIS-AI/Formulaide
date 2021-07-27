package formulaide.ui.screens

import formulaide.api.data.Form
import formulaide.api.data.Record
import formulaide.api.data.RecordState
import formulaide.api.types.Ref.Companion.load
import formulaide.client.Client
import formulaide.client.routes.findSubmission
import formulaide.client.routes.todoListFor
import formulaide.ui.components.styledCard
import formulaide.ui.components.useAsync
import formulaide.ui.reportExceptions
import formulaide.ui.traceRenders
import formulaide.ui.useClient
import formulaide.ui.utils.text
import react.*
import react.dom.p

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

	styledCard(
		"Liste des dossiers",
		"${form.name}, dossiers ${state.displayName()}",
		"Actualiser" to {
			records = client.todoListFor(form, state)
		},
	) {
		p { text("${records.size} dossiers sont chargés. Pour des raisons de performance, il n'est pas possible de charger plus de ${Record.MAXIMUM_NUMBER_OF_RECORDS_PER_ACTION} dossiers à la fois.") }
	}

	for (record in records) {
		child(ReviewRecord) {
			attrs {
				this.form = form
				this.record = record
			}
		}
	}

}

private external interface ReviewRecordProps : RProps {
	var form: Form
	var record: Record
}

private val ReviewRecord = fc<ReviewRecordProps> { props ->
	val form = props.form
	val record = props.record
	val scope = useAsync()
	val (client) = useClient()

	record.form.load(form)
	require(client is Client.Authenticated) { "Seuls les employés peuvent accéder à cette page" }

	var refreshSubmission by useState(0)
	useEffectOnce {
		for (submission in record.submissions)
			scope.reportExceptions {
				submission.load { client.findSubmission(it) }
				refreshSubmission++
			}
	}

	styledCard(
		"Dossier",
	) {
		for (submission in record.submissions) {
			if (submission.loaded) {
				p { text(submission.obj.data.toString()) }
			}
		}
	}
}
