package formulaide.ui.screens

import formulaide.api.data.CompositeMetadata
import formulaide.client.Client
import formulaide.client.routes.editData
import formulaide.ui.*
import formulaide.ui.Role.Companion.role
import formulaide.ui.components.StyledButton
import formulaide.ui.components.cards.Card
import formulaide.ui.components.styledCheckbox
import formulaide.ui.components.styledField
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

		if (user.role >= Role.EMPLOYEE) styledField("hide-disabled", "Groupes archivés") {
			styledCheckbox("hide-disabled", "Afficher les groupes archivés") {
				onChange = { showArchived = it.target.checked }
			}
		}

		for (composite in composites.filter { showArchived || it.open }) {
			styledField("composite-${composite.id}", composite.name) {
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
