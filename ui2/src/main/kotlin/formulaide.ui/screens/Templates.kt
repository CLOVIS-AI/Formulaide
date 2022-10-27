package formulaide.ui.screens

import androidx.compose.runtime.*
import formulaide.ui.components.*
import formulaide.ui.navigation.Screen
import formulaide.ui.navigation.client
import formulaide.ui.utils.rememberRef
import formulaide.ui.utils.rememberState
import opensavvy.formulaide.core.Template
import opensavvy.formulaide.core.User
import opensavvy.state.Slice.Companion.valueOrNull
import opensavvy.state.firstResultOrNull
import org.jetbrains.compose.web.dom.Text

val TemplateList: Screen = Screen(
	title = "Modèles",
	requiredRole = User.Role.EMPLOYEE,
	"?templates",
	icon = "ri-clipboard-line",
	iconSelected = "ri-clipboard-fill"
) {
	var showArchived by remember { mutableStateOf(false) }
	var forceRefresh by remember { mutableStateOf(0) }

	val templates by rememberState(
		client,
		forceRefresh,
		showArchived
	) { client.templates.list(includeClosed = showArchived) }

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
						forceRefresh++
					}
				}
			}

			DisplayError(templates)
		}
	) {
		for (template in templates.valueOrNull ?: emptyList()) {
			ShowTemplate(template)
		}
	}
}

@Composable
private fun ShowTemplate(ref: Template.Ref) {
	val slice by rememberRef(ref)
	val template = slice.valueOrNull

	Paragraph(template?.name ?: "", loading = slice.progression) {
		Text("${template?.versions?.size ?: "1"} versions")

		ButtonContainer {
			TextButton(
				{ client.templates.edit(ref, open = !template!!.open).firstResultOrNull() },
				enabled = template != null
			) {
				Text(if (template?.open != false) "Archiver" else "Désarchiver")
			}
		}

		DisplayError(slice)
	}
}
