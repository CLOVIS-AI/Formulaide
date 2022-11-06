package formulaide.ui.screens

import androidx.compose.runtime.*
import formulaide.ui.components.*
import formulaide.ui.navigation.Screen
import formulaide.ui.navigation.client
import formulaide.ui.navigation.currentScreen
import formulaide.ui.theme.RailButton
import formulaide.ui.utils.*
import opensavvy.formulaide.core.Template
import opensavvy.formulaide.core.User
import opensavvy.state.slice.valueOrNull
import org.jetbrains.compose.web.dom.Text

val TemplateList: Screen = Screen(
	title = "Modèles",
	requiredRole = User.Role.EMPLOYEE,
	"?templates",
	icon = "ri-clipboard-line",
	iconSelected = "ri-clipboard-fill",
	actions = {
		if (client.context.value.role >= User.Role.ADMINISTRATOR) {
			RailButton(
				TemplateCreator.icon,
				TemplateCreator.iconSelected,
				"Créer un formulaire",
				selected = false,
			) { currentScreen = TemplateCreator }
		}
	}
) {
	var showArchived by remember { mutableStateOf(false) }

	val (templates, refresh) = rememberSlice(client, showArchived) {
		client.templates.list(includeClosed = showArchived)
	}

	Page(
		title = "Modèles",
		header = {
			ChipContainerContainer {
				ChipContainer {
					FilterChip("Archivés", showArchived, onUpdate = { showArchived = it })
				}

				ChipContainer {
					RefreshButton {
						client.templates.cache.expireAll()
						refresh()
					}
				}
			}

			DisplayError(templates)
		}
	) {
		for (template in templates?.valueOrNull ?: emptyList()) {
			ShowTemplate(template)
		}
	}
}

@Composable
private fun ShowTemplate(ref: Template.Ref) {
	val slice by rememberRef(ref)
	val template = slice?.valueOrNull

	Paragraph(template?.name ?: "") {
		Text("${template?.versions?.size ?: "1"} versions")

		val failure = rememberPossibleFailure()

		ButtonContainer {
			if (client.role >= User.Role.ADMINISTRATOR) {
				TextButton(
					{ currentScreen = TemplateNewVersion(ref) },
					enabled = template != null
				) {
					Text("Nouvelle version")
				}

				TextButton(
					{ client.templates.edit(ref, open = !template!!.open).orReport(failure) },
					enabled = template != null
				) {
					Text(if (template?.open != false) "Archiver" else "Désarchiver")
				}
			}
		}

		DisplayError(slice)
		DisplayError(failure)
	}
}
