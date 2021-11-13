package formulaide.ui.screens

import formulaide.api.data.CompositeMetadata
import formulaide.client.Client
import formulaide.client.routes.editData
import formulaide.ui.*
import formulaide.ui.Role.Companion.role
import formulaide.ui.components.styledButton
import formulaide.ui.components.styledCard
import formulaide.ui.components.styledCheckbox
import formulaide.ui.components.styledField
import formulaide.ui.utils.map
import kotlinx.html.js.onChangeFunction
import org.w3c.dom.HTMLInputElement
import react.Props
import react.fc
import react.useState

val DataList = fc<Props>("DataList") {
	traceRenders("DataList")

	val (client) = useClient()
	val user by useUser()
	val composites by useAllComposites()
		.map { composites -> composites.sortedBy { it.name } }

	var showArchived by useState(false)

	styledCard(
		"Groupes",
		null,
		contents = {
			if (user.role >= Role.EMPLOYEE) styledField("hide-disabled", "Groupes archivés") {
				styledCheckbox("hide-disabled", "Afficher les groupes archivés") {
					onChangeFunction =
						{ showArchived = (it.target as HTMLInputElement).checked }
				}
			}

			for (composite in composites.filter { showArchived || it.open }) {
				styledField("composite-${composite.id}", composite.name) {
					if (user.role >= Role.ADMINISTRATOR) {
						require(client is Client.Authenticated) { "Le client devrait être connecté." }

						styledButton("Copier", action = { navigateTo(Screen.NewData(composite)) })

						styledButton(
							if (composite.open) "Archiver" else "Désarchiver",
							action = {
								client.editData(CompositeMetadata(
									composite.id,
									!composite.open,
								))
								refreshComposites()
							}
						)
					}
				}
			}
		}
	)

}
