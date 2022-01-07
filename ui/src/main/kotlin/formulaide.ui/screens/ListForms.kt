package formulaide.ui.screens

import formulaide.api.data.Form
import formulaide.api.data.FormMetadata
import formulaide.api.data.Record
import formulaide.api.data.RecordState
import formulaide.api.types.Ref.Companion.createRef
import formulaide.client.Client
import formulaide.client.routes.editForm
import formulaide.client.routes.listClosedForms
import formulaide.client.routes.todoListFor
import formulaide.ui.*
import formulaide.ui.Role.Companion.role
import formulaide.ui.components.StyledButton
import formulaide.ui.components.cards.Card
import formulaide.ui.components.cards.action
import formulaide.ui.components.inputs.Checkbox
import formulaide.ui.components.inputs.Field
import formulaide.ui.components.inputs.Nesting
import formulaide.ui.components.text.Text
import formulaide.ui.components.useAsync
import formulaide.ui.components.useAsyncEffect
import formulaide.ui.utils.*
import formulaide.ui.utils.DelegatedProperty.Companion.asDelegated
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import react.*
import react.dom.html.ReactHTML.div

private typealias RecordKey = Pair<Form, RecordState>

private val recordsCache = HashMap<RecordKey, GlobalState<List<Record>>>()
private val recordsCacheModification = GlobalState(0)
fun clearRecords() {
	recordsCache.clear()
	recordsCacheModification.value++
}

fun CoroutineScope.insertIntoRecordsCache(
	client: Client.Authenticated,
	form: Form,
	state: RecordState,
	records: List<Record>,
) {
	val list = getRecords(client, form, state)
	list.asDelegated().useListEquality().useEquals().update { records }
}

private fun CoroutineScope.getRecords(
	client: Client.Authenticated,
	form: Form,
	state: RecordState,
) = recordsCache.getOrPut(form to state) {
	GlobalState<List<Record>>(emptyList()).apply {
		reportExceptions {
			this@apply.value = client.todoListFor(form, state)
			recordsCacheModification.value++
		}
	}
}

private fun CoroutineScope.getRecords(
	client: Client.Authenticated,
	form: Form,
) = (form.actions.map { RecordState.Action(it.createRef()) } + RecordState.Refused)
	.map { getRecords(client, form, it) }
	.flatMap { it.value }

val FormList = FC<Props>("FormList") {
	traceRenders("FormList")

	val (client) = useClient("FormList client")
	val user by useUser("FormList user")

	val forms by useForms()
		.map { forms -> forms.sortedBy { it.name } }
	val scope = useAsync()

	useEffectOnce {
		scope.launch {
			while (true) {
				delay(1000L * 60 * 10)
				clearRecords()
			}
		}
	}

	var archivedForms by useState(emptyList<Form>()).asDelegated()
		.useListEquality()
		.useEquals()
	var showArchivedForms by useState(false)
	useAsyncEffect(showArchivedForms) {
		if (showArchivedForms) {
			require(client is Client.Authenticated) { "Il n'est pas possible d'appuyer sur ce bouton sans être connecté." }
			archivedForms = client.listClosedForms()
		}
	}

	val shownForms = useMemo(forms, archivedForms, showArchivedForms) {
		if (showArchivedForms)
			forms + archivedForms
		else
			forms
	}

	Card {
		title = "Formulaires"

		action("Actualiser") {
			refreshForms()
			clearRecords()
		}

		if (user.role >= Role.EMPLOYEE) Field {
			id = "hide-disabled"
			text = "Formulaires archivés"

			Checkbox {
				id = "hide-disabled"
				text = "Afficher les formulaires archivés"
				onChange = { showArchivedForms = it.target.checked }
			}
		}

		for (form in shownForms) {
			FormDescription {
				key = form.id
				this.form = form
			}
		}
	}
}

internal external interface FormDescriptionProps : Props {
	var form: Form
}

internal val FormDescription = memo(FC<FormDescriptionProps>("FormDescription") { props ->
	val form = props.form
	val user by useUser()

	var showRecords by useState(false)
	var showAdministration by useState(false)

	val (client) = useClient()

	fun toggle(bool: Boolean) = if (!bool) "▼" else "▲"

	div {
		Text { text = form.name }

		StyledButton {
			text = "Remplir"
			action = { navigateTo(Screen.SubmitForm(form.createRef())) }
		}

		if (user.role >= Role.EMPLOYEE)
			StyledButton {
				text = "Dossiers ${toggle(showRecords)}"
				action = { showRecords = !showRecords }
			}

		if (user.role >= Role.EMPLOYEE)
			StyledButton {
				text = "Gestion ${toggle(showAdministration)}"
				action = { showAdministration = !showAdministration }
			}
	}

	if (showRecords) Nesting {
		Text { text = "Dossiers :" }

		for (action in form.actions.sortedBy { it.order }) {
			ActionDescription {
				key = form.id + "-" + action.id
				this.form = form
				this.state = RecordState.Action(action.createRef())
			}
		}

		ActionDescription {
			this.form = form
			this.state = RecordState.Refused
		}

		ActionDescription {
			this.form = form
			this.state = null
		}
	}

	if (showAdministration) Nesting {
		Text { text = "Gestion :" }

		if (user.role >= Role.ADMINISTRATOR) {
			require(client is Client.Authenticated) // not possible otherwise

			StyledButton {
				text = "Copier"
				action = { navigateTo(Screen.NewForm(form, copy = true)) }
			}

			StyledButton {
				text = "Modifier"
				action = { navigateTo(Screen.NewForm(form, copy = false)) }
			}

			StyledButton {
				text = if (form.public) "Rendre interne" else "Rendre public"
				action = {
					client.editForm(FormMetadata(form.createRef(), public = !form.public))
					refreshForms()
				}
			}

			StyledButton {
				text = if (form.open) "Archiver" else "Désarchiver"
				action = {
					client.editForm(FormMetadata(form.createRef(), open = !form.open))
					refreshForms()
				}
			}
		}

		StyledButton {
			text = "Voir HTML"
			action = {
				window.open("${client.hostUrl}/forms/html?id=${form.id}&url=${client.hostUrl}")
			}
		}
	}
})

internal external interface ActionDescriptionProps : Props {
	var form: Form
	var state: RecordState?
}

internal val ActionDescription = FC<ActionDescriptionProps>("ActionDescription") { props ->
	val form = props.form
	val state = props.state

	val (client) = useClient()
	val user by useUser()
	val scope = useAsync()
	require(client is Client.Authenticated) { "Seuls les employés peuvent afficher 'ActionDescription'" }

	val recordsCacheEdits by useGlobalState(recordsCacheModification) // to force a render when the cache changes

	val recordsDelegate =
		if (state != null) useGlobalState(scope.getRecords(client, form, state))
		else useMemo(recordsCacheEdits) { ReadDelegatedProperty { scope.getRecords(client, form) } }

	val records by recordsDelegate

	val stateName = when (state) {
		is RecordState.Action -> state.displayName()
			.takeIf { user?.service?.id == state.current.obj.reviewer.id }
		else -> state.displayName()
	}
	if (stateName != null) {
		val title = stateName +
				when {
					records.size == 1 -> " (1 dossier)"
					records.size == Record.MAXIMUM_NUMBER_OF_RECORDS_PER_ACTION -> " (${Record.Companion.MAXIMUM_NUMBER_OF_RECORDS_PER_ACTION}+ dossiers)"
					records.isNotEmpty() -> " (${records.size} dossiers)"
					else -> ""
				}

		StyledButton {
			text = title
			action = { navigateTo(Screen.Review(form, state, records)) }
		}
	}
}
