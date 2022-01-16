package formulaide.ui.screens.forms.list

import formulaide.api.data.Record
import formulaide.api.data.RecordState
import formulaide.client.Client
import formulaide.ui.Screen
import formulaide.ui.components.StyledButton
import formulaide.ui.components.useAsync
import formulaide.ui.navigateTo
import formulaide.ui.screens.displayName
import formulaide.ui.useClient
import formulaide.ui.useUser
import formulaide.ui.utils.ReadDelegatedProperty
import formulaide.ui.utils.useGlobalState
import react.FC
import react.useMemo

external interface ActionDescriptionProps : FormDescriptionProps {
	var state: RecordState?
}

val ActionDescription = FC<ActionDescriptionProps>("ActionDescription") { props ->
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
