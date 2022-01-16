package formulaide.ui.screens.forms.list

import formulaide.api.data.Form
import formulaide.api.data.Record
import formulaide.api.data.RecordState
import formulaide.api.types.Ref.Companion.createRef
import formulaide.client.Client
import formulaide.client.routes.todoListFor
import formulaide.ui.reportExceptions
import formulaide.ui.utils.GlobalState
import formulaide.ui.utils.useEquals
import formulaide.ui.utils.useListEquality
import kotlinx.coroutines.CoroutineScope

private typealias RecordKey = Pair<Form, RecordState>

private val recordsCache = HashMap<RecordKey, GlobalState<List<Record>>>()
val recordsCacheModification = GlobalState(0)

fun clearRecords() {
	recordsCache.clear()
	recordsCacheModification.value++
}

fun CoroutineScope.getRecords(
	client: Client.Authenticated,
	form: Form,
	state: RecordState,
): GlobalState<List<Record>> = recordsCache.getOrPut(form to state) {
	GlobalState<List<Record>>(emptyList()).apply {
		reportExceptions {
			this@apply.value = client.todoListFor(form, state)
			recordsCacheModification.value++
		}
	}
}

fun CoroutineScope.getRecords(
	client: Client.Authenticated,
	form: Form,
) = (form.actions.map { RecordState.Action(it.createRef()) } + RecordState.Refused)
	.map { getRecords(client, form, it) }
	.flatMap { it.value }

fun CoroutineScope.insertIntoRecordsCache(
	client: Client.Authenticated,
	form: Form,
	state: RecordState,
	records: List<Record>,
) {
	val list = getRecords(client, form, state)
	list.asDelegated()
		.useListEquality()
		.useEquals()
		.update { records }
}
