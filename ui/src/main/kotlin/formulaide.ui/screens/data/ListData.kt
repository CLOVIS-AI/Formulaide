package formulaide.ui.screens.data

import formulaide.api.data.CompositeMetadata
import formulaide.client.Client
import formulaide.client.routes.editData
import formulaide.ui.*
import formulaide.ui.Role.Companion.role
import formulaide.ui.components.StyledButton
import formulaide.ui.components.cards.Card
import formulaide.ui.components.inputs.Checkbox
import formulaide.ui.components.inputs.Field
import formulaide.ui.utils.map
import react.FC
import react.Props
import react.useState

val DataList = FC<Props>("DataList") {
	traceRenders("DataList")

	val (client) = useClient()
	val user by useUser()
	val composites by useAllComposites()
		.map { composites -> composites.sortedBy { it.name } }

	var showArchived by useState(false)

	Card {
		title = "Groupes"

		if (user.role >= Role.EMPLOYEE) Field {
			id = "hide-disabled"
			text = "Groupes archivés"

			Checkbox {
				id = "hide-disabled"
				text = "Afficher les groupes archivés"
				onChange = { showArchived = it.target.checked }
			}
		}

		for (composite in composites.filter { showArchived || it.open }) {
			Field {
				id = "composite-${composite.id}"
				text = composite.name

				if (user.role >= Role.ADMINISTRATOR) {
					require(client is Client.Authenticated) { "Le client devrait être connecté." }

					StyledButton {
						text = "Copier"
						action = { navigateTo(Screen.NewData(composite)) }
					}

					StyledButton {
						text = if (composite.open) "Archiver" else "Désarchiver"
						action = {
							client.editData(CompositeMetadata(
								composite.id,
								!composite.open,
							))
							refreshComposites()
						}
					}
				}
			}
		}
	}
}
