package formulaide.ui.screens

import androidx.compose.runtime.*
import formulaide.ui.components.*
import formulaide.ui.navigation.Screen
import formulaide.ui.navigation.client
import formulaide.ui.navigation.currentScreen
import formulaide.ui.theme.RailButton
import formulaide.ui.utils.rememberRef
import formulaide.ui.utils.rememberState
import formulaide.ui.utils.role
import opensavvy.formulaide.core.User
import opensavvy.state.Slice.Companion.valueOrNull
import opensavvy.state.firstResultOrNull
import org.jetbrains.compose.web.dom.Text

val FormList: Screen = Screen(
	title = "Formulaires",
	requiredRole = User.Role.ANONYMOUS,
	"?forms",
	icon = "ri-survey-line",
	iconSelected = "ri-survey-fill",
	actions = {
		if (client.context.value.role >= User.Role.ADMINISTRATOR) {
			RailButton(
				FormEditor.icon,
				FormEditor.iconSelected,
				"Créer un formulaire",
				selected = false,
			) { currentScreen = FormEditor }
		}
	}
) {
	val role = client.role

	var showArchived by remember { mutableStateOf(false) }
	var showPrivate by remember { mutableStateOf(role >= User.Role.EMPLOYEE) }
	var forceRefresh by remember { mutableStateOf(0) }

	val forms by rememberState(
		client,
		showArchived,
		showPrivate,
		forceRefresh,
	) { client.forms.list(includeClosed = showArchived, includePrivate = showPrivate) }

	Page(
		"Formulaires",
		header = {
			ChipContainerContainer {
				if (client.role >= User.Role.EMPLOYEE) {
					ChipContainer {
						FilterChip("Privés", showPrivate, onUpdate = { showPrivate = it })

						if (client.role >= User.Role.ADMINISTRATOR) {
							FilterChip("Archivés", showArchived, onUpdate = { showArchived = it })
						}
					}
				}

				ChipContainer {
					RefreshButton {
						client.forms.cache.expireAll()
						forceRefresh++
					}
				}
			}

			DisplayError(forms)
		}
	) {
		for (form in forms.valueOrNull ?: emptyList()) {
			ShowForm(form)
		}
	}
}

@Composable
private fun ShowForm(ref: opensavvy.formulaide.core.Form.Ref) {
	val slice by rememberRef(ref)
	val form = slice.valueOrNull

	Paragraph(form?.name ?: "", loading = slice.progression) {
		Text("${form?.versions?.size ?: 1} versions")

		ButtonContainer {
			TextButton(
				{ client.forms.edit(ref, public = !form!!.public).firstResultOrNull() },
				enabled = form != null
			) {
				Text(if (form?.public != false) "Rendre privé" else "Rendre public")
			}

			TextButton(
				{ client.forms.edit(ref, open = !form!!.open).firstResultOrNull() },
				enabled = form != null,
			) {
				Text(if (form?.open != false) "Archiver" else "Désarchiver")
			}
		}

		DisplayError(slice)
	}
}
