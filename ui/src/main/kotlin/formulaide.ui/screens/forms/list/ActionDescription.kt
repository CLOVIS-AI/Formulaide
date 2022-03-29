package formulaide.ui.screens.forms.list

import formulaide.api.data.Record
import formulaide.api.data.RecordState
import formulaide.api.users.canAccess
import formulaide.client.Client
import formulaide.ui.Screen
import formulaide.ui.components.LoadingSpinner
import formulaide.ui.components.StyledButton
import formulaide.ui.components.text.ErrorText
import formulaide.ui.components.useAsync
import formulaide.ui.navigateTo
import formulaide.ui.screens.review.displayName
import formulaide.ui.useClient
import formulaide.ui.useUser
import formulaide.ui.utils.ReadDelegatedProperty
import formulaide.ui.utils.useGlobalState
import react.FC
import react.dom.html.ReactHTML.p
import react.useMemo

external interface ActionDescriptionProps : FormDescriptionProps {
	/**
	 * - [RecordState.Action]: this chip represents that action
	 * - [RecordState.Refused]: this chip represents the 'refused records' page
	 * - `null`: this chip represents the 'all records' page
	 */
	var state: RecordState?
}

/**
 * Displays a button allowing the user to navigate to the review page for a specific [RecordState].
 * Also displays the number of records in that state.
 *
 * If the user is not allowed to access those records, displays nothing.
 */
val ActionDescription = FC<ActionDescriptionProps>("ActionDescription") { props ->
	val form = props.form
	val state = props.state

	val (client) = useClient()
	val (user) = useUser()

	if (user == null) {
		p { +"Récupération de vos accès…" }
		LoadingSpinner()
		return@FC
	}

	if (client !is Client.Authenticated) {
		ErrorText { text = "Seuls les employés peuvent afficher les descriptions des étapes" }
		return@FC
	}

	if (user.canAccess(form, state))
		ActionDescriptionDisplay { +props }
}

private val ActionDescriptionDisplay = FC<ActionDescriptionProps>("ActionDescriptionDisplay") { props ->
	val form = props.form
	val state = props.state
	val scope = useAsync()
	val (client) = useClient()

	require(client is Client.Authenticated)

	val recordsCacheEdits by useGlobalState(recordsCacheModification) // to force a render when the cache changes

	val recordsDelegate =
		if (state != null) useGlobalState(scope.getRecords(client, form, state))
		else useMemo(recordsCacheEdits) { ReadDelegatedProperty { scope.getRecords(client, form) } }

	val records by recordsDelegate

	val title = state.displayName() +
			when {
				records.size == 1 -> " (1 dossier)"
				records.size == Record.MAXIMUM_NUMBER_OF_RECORDS_PER_ACTION -> " (${Record.Companion.MAXIMUM_NUMBER_OF_RECORDS_PER_ACTION}+ dossiers)"
				records.isNotEmpty() -> " (${records.size} dossiers)"
				else -> ""
			}

	StyledButton {
		text = title
		action = { navigateTo(Screen.Review(form, state)) }
	}
}
