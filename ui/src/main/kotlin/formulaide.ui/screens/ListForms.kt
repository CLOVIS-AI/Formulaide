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
import formulaide.ui.components.*
import formulaide.ui.utils.*
import formulaide.ui.utils.DelegatedProperty.Companion.asDelegated
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.html.js.onChangeFunction
import org.w3c.dom.HTMLInputElement
import react.*
import react.dom.div

private typealias RecordKey = Pair<Form, RecordState>

private val recordsCache = HashMap<RecordKey, GlobalState<List<Record>>>()
fun clearRecords() {
	recordsCache.clear()
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
		}
	}
}

val FormList = fc<RProps> { _ ->
	traceRenders("FormList")

	val (client) = useClient()
	val user by useUser()

	val forms by useForms()
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
		require(client is Client.Authenticated) { "Il n'est pas possible d'appuyer sur ce bouton sans être connecté." }
		if (showArchivedForms)
			archivedForms = client.listClosedForms()
	}

	val shownForms = useMemo(forms, archivedForms, showArchivedForms) {
		if (showArchivedForms)
			forms + archivedForms
		else
			forms
	}

	styledCard(
		"Formulaires",
		null,
		"Actualiser" to {
			refreshForms()
			clearRecords()
		},
		contents = {
			if (user.role >= Role.EMPLOYEE) styledField("hide-disabled", "Formulaires archivés") {
				styledCheckbox("hide-disabled", "Afficher les formulaires archivés") {
					onChangeFunction =
						{ showArchivedForms = (it.target as HTMLInputElement).checked }
				}
			}

			for (form in shownForms) {
				child(FormDescription) {
					attrs {
						key = form.id
						this.form = form
					}
				}
			}
		}
	)

}

internal external interface FormDescriptionProps : RProps {
	var form: Form
}

internal val FormDescription = memo(fc<FormDescriptionProps> { props ->
	val form = props.form
	val user by useUser()

	var showRecords by useState(false)
	var showAdministration by useState(false)

	val (client) = useClient()

	fun toggle(bool: Boolean) = if (!bool) "▼" else "▲"

	div {
		text(form.name)

		styledButton("Remplir") { navigateTo(Screen.SubmitForm(form.createRef())) }

		if (user.role >= Role.EMPLOYEE)
			styledButton("Dossiers ${toggle(showRecords)}") { showRecords = !showRecords }

		if (user.role >= Role.EMPLOYEE)
			styledButton("Gestion ${toggle(showAdministration)}") {
				showAdministration = !showAdministration
			}
	}

	if (showRecords) styledNesting {
		text("Dossiers :")

		for (action in form.actions.sortedBy { it.order }) {
			child(ActionDescription) {
				attrs {
					key = form.id + "-" + action.id
					this.form = form
					this.state = RecordState.Action(action.createRef())
				}
			}
		}

		child(ActionDescription) {
			attrs {
				this.form = form
				this.state = RecordState.Refused
			}
		}
	}

	if (showAdministration) styledNesting {
		text("Gestion :")

		if (user.role >= Role.ADMINISTRATOR) {
			require(client is Client.Authenticated) // not possible otherwise

			styledButton("Copier", action = { navigateTo(Screen.NewForm(form)) })

			styledButton(if (form.public) "Rendre interne" else "Rendre public", action = {
				client.editForm(FormMetadata(form.createRef(),
				                             public = !form.public))
				refreshForms()
			})

			styledButton(if (form.open) "Archiver" else "Désarchiver", action = {
				client.editForm(FormMetadata(form.createRef(),
				                             open = !form.open))
				refreshForms()
			})
		}

		styledButton("Voir HTML", action = {
			window.open("${client.hostUrl}/forms/html?id=${form.id}&url=${client.hostUrl}")
		})
	}
})

internal external interface ActionDescriptionProps : RProps {
	var form: Form
	var state: RecordState
}

internal val ActionDescription = fc<ActionDescriptionProps> { props ->
	val form = props.form
	val state = props.state

	val (client) = useClient()
	val user by useUser()
	val scope = useAsync()
	require(client is Client.Authenticated) { "Seuls les employés peuvent afficher 'ActionDescription'" }

	val records by useGlobalState(scope.getRecords(client, form, state))

	val stateName = when (state) {
		is RecordState.Action -> state.displayName()
			.takeIf { user?.service?.id == state.current.obj.reviewer.id }
		is RecordState.Refused -> state.displayName()
	}
	if (stateName != null) {
		val title = stateName +
				when {
					records.size == 1 -> " (1 dossier)"
					records.size == Record.MAXIMUM_NUMBER_OF_RECORDS_PER_ACTION -> " (${Record.Companion.MAXIMUM_NUMBER_OF_RECORDS_PER_ACTION}+ dossiers)"
					records.isNotEmpty() -> " (${records.size} dossiers)"
					else -> ""
				}

		styledButton(title, action = { navigateTo(Screen.Review(form, state, records)) })
	}
}
