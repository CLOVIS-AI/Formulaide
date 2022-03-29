package formulaide.ui.screens.forms.list

import formulaide.api.data.Form
import formulaide.api.data.FormMetadata
import formulaide.api.data.RecordState
import formulaide.api.types.Ref.Companion.createRef
import formulaide.api.users.canAccess
import formulaide.client.Client
import formulaide.client.routes.editForm
import formulaide.ui.*
import formulaide.ui.Role.Companion.role
import formulaide.ui.components.StyledButton
import formulaide.ui.components.inputs.Nesting
import formulaide.ui.components.useAsync
import formulaide.ui.utils.ReadDelegatedProperty
import formulaide.ui.utils.useGlobalState
import kotlinx.browser.window
import react.*
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.span

external interface FormDescriptionProps : Props {
	var form: Form
}

val FormDescription = FC<FormDescriptionProps>("FormDescription") { props ->
	val form = props.form
	val user by useUser()

	var showRecords by useState(false)
	var showAdministration by useState(false)

	val (client) = useClient()
	val scope = useAsync()

	val recordsCacheEdits by useGlobalState(recordsCacheModification) // to force a render when the cache changes
	val recordsDelegate = useMemo(recordsCacheEdits) {
		if (client is Client.Authenticated) ReadDelegatedProperty { scope.getRecords(client, form) }
		else ReadDelegatedProperty { emptyList() }
	}
	val records by recordsDelegate

	fun toggle(bool: Boolean) = if (!bool) "▼" else "▲"

	div {
		+form.name

		StyledButton {
			text = "Remplir"
			action = { navigateTo(Screen.SubmitForm(form.createRef())) }
		}

		if (user.role >= Role.EMPLOYEE && user?.canAccess(form, null) == true)
			StyledButton {
				text = when {
					records.isEmpty() -> "Dossiers "
					records.size == 1 -> "1 Dossier "
					else -> "${records.size} Dossiers"
				} + toggle(showRecords)

				action = { showRecords = !showRecords }
			}

		if (user.role >= Role.EMPLOYEE)
			StyledButton {
				text = "Gestion ${toggle(showAdministration)}"
				action = { showAdministration = !showAdministration }
			}
	}

	if (showRecords) Nesting {
		+"Dossiers :"

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
		+"Gestion :"

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

		if (form.public) {
			StyledButton {
				text = "Voir HTML"
				action = {
					window.open("${client.hostUrl}/forms/html?id=${form.id}&url=${client.hostUrl}")
				}
			}
		} else {
			span { +" L'export HTML n'est disponible que pour les formulaires publics " }
		}
	}
}
