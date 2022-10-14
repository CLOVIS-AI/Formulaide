package formulaide.ui.screens.forms.list

import formulaide.api.data.Form
import formulaide.api.users.User.Companion.role
import formulaide.client.Client
import formulaide.client.routes.listClosedForms
import formulaide.core.User
import formulaide.ui.components.cards.Card
import formulaide.ui.components.cards.action
import formulaide.ui.components.inputs.Checkbox
import formulaide.ui.components.inputs.Field
import formulaide.ui.components.useAsyncEffect
import formulaide.ui.components.useAsyncEffectOnce
import formulaide.ui.refreshForms
import formulaide.ui.useClient
import formulaide.ui.useForms
import formulaide.ui.useUser
import formulaide.ui.utils.DelegatedProperty.Companion.asDelegated
import formulaide.ui.utils.useEquals
import formulaide.ui.utils.useListEquality
import kotlinx.coroutines.delay
import react.*

val FormList = FC<Props>("FormList") {
	val (client) = useClient()
	val user by useUser()

	val unsortedForms by useForms()
	val forms = useMemo(unsortedForms) { unsortedForms.sortedBy { it.name } }

	// If the user keeps this window opened for time, refresh it automatically
	useAsyncEffectOnce { // Restarted on remounts, so no need for a loop
		delay(1000L * 60 * 10) // 10 minutes
		clearRecords()
	}

	var archivedForms by useState(emptyList<Form>()).asDelegated()
		.useListEquality()
		.useEquals()
	var showArchivedForms by useState(false)

	useAsyncEffect(showArchivedForms) {
		archivedForms = if (showArchivedForms) {
			require(client is Client.Authenticated) { "Il n'est pas possible d'afficher les formulaires archivés sans être connecté." }
			client.listClosedForms()
		} else {
			emptyList()
		}
	}

	val listedForms = useMemo(forms, archivedForms) { forms + archivedForms }

	Card {
		title = "Formulaires"

		action("Actualiser") {
			refreshForms()
			clearRecords()
		}

		if (user.role >= User.Role.EMPLOYEE) Field {
			id = "hide-disabled"
			text = "Formulaires archivés"

			Checkbox {
				id = "hide-disabled"
				text = "Afficher les formulaires archivés"
				onChange = { showArchivedForms = it.target.checked }
			}
		}

		for (form in listedForms) {
			FormDescription {
				key = form.id
				this.form = form
			}
		}
	}
}
