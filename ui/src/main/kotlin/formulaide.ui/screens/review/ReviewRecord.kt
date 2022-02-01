package formulaide.ui.screens.review

import formulaide.api.data.ParsedSubmission
import formulaide.api.data.Record
import formulaide.api.data.RecordStateTransition
import formulaide.api.fields.FormField
import formulaide.api.types.Ref.Companion.load
import formulaide.client.Client
import formulaide.client.routes.findSubmission
import formulaide.ui.*
import formulaide.ui.components.cards.Card
import formulaide.ui.components.useAsync
import formulaide.ui.utils.replace
import kotlinx.coroutines.launch
import react.FC
import react.dom.html.ReactHTML.tr
import react.useEffect
import react.useState

internal data class ParsedTransition(
	val transition: RecordStateTransition,
	val submission: ParsedSubmission?,
)

external interface ReviewRecordProps : RecordTableProps {
	var record: Record
	var columnsToDisplay: List<Pair<String, FormField>>
}

val ReviewRecord = FC<ReviewRecordProps>("ReviewRecord") { props ->
	props.record.form.load(props.form)
	props.record.load()

	val scope = useAsync()
	val (client) = useClient()
	val user by useUser()
	require(client is Client.Authenticated) { "Cannot display a ReviewRecord for an unauthenticated user" }

	var showFullHistory by useState(false)
	fun getHistory() = props.record.history.map { ParsedTransition(it, null) }
	val (fullHistory, setFullHistory) = useState(getHistory())
	val history =
		if (showFullHistory) fullHistory
			.sortedBy { it.transition.timestamp }
		else fullHistory.groupBy { it.transition.previousState }
			.mapNotNull { (_, v) -> v.maxByOrNull { it.transition.timestamp } }
			.sortedBy { it.transition.timestamp }

	useEffect(props.record) {
		reportExceptions {
			val newHistory = getHistory()
			if (newHistory.map { it.transition } != fullHistory.map { it.transition }) {
				setFullHistory(newHistory)
			}
		}
	}

	useEffect(fullHistory, showFullHistory) {
		reportExceptions {
			for ((i, parsed) in history.withIndex()) {
				traceRenders("ReviewRecord … parsing transition $parsed")
				val fields = parsed.transition.fields
				if (fields != null && parsed.submission == null) {
					scope.launch {
						fields.load { client.findSubmission(it) }
						val newParsed = parsed.copy(submission = fields.obj.parse(props.form))
						setFullHistory { full -> full.replace(i, newParsed) }
					}
				}
			}
		}
	}

	if (user == null) {
		Card {
			title = "Dossier"
			loading = true
			+"Chargement de l'utilisateur…"
		}
		return@FC
	}

	tr {
		CrashReporter {
			if (props.expandedRecords[props.record] == false) {
				ReviewRecordCollapsed {
					+props
					this.history = history
					this.showFullHistory = showFullHistory
				}
			} else {
				ReviewRecordExpanded {
					+props
					this.history = history
					this.showFullHistory = showFullHistory
					this.updateShowFullHistory = { showFullHistory = it }
				}
			}
		}
	}
}
