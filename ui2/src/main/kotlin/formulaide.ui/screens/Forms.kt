package formulaide.ui.screens

import androidx.compose.runtime.*
import formulaide.ui.components.*
import formulaide.ui.navigation.Screen
import formulaide.ui.navigation.client
import formulaide.ui.navigation.currentScreen
import formulaide.ui.theme.RailButton
import formulaide.ui.utils.*
import opensavvy.formulaide.core.User
import opensavvy.state.slice.valueOrNull
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
				FormCreator.icon,
				FormCreator.iconSelected,
				"Créer un formulaire",
				selected = false,
			) { currentScreen = FormCreator }
		}
	}
) {
	val role = client.role

	var showArchived by remember { mutableStateOf(false) }
	var showPrivate by remember { mutableStateOf(role >= User.Role.EMPLOYEE) }

	val (forms, refresh) = rememberSlice(
		client,
		showArchived,
		showPrivate
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
						refresh()
					}
				}
			}

			DisplayError(forms)
		}
	) {
		for (form in forms?.valueOrNull ?: emptyList()) {
			ShowForm(form)
		}
	}
}

@Composable
private fun ShowForm(ref: opensavvy.formulaide.core.Form.Ref) {
	val slice by rememberRef(ref)
	val form = slice?.valueOrNull

	Paragraph(form?.name ?: "") {
		Text("${form?.versions?.size ?: 1} versions")

		val failure = rememberPossibleFailure()

		ButtonContainer {
			TextButton(
				{ currentScreen = FormNewVersion(ref) }
			) {
				Text("Nouvelle version")
			}

			TextButton(
				{ client.forms.edit(ref, public = !form!!.public).orReport(failure) },
				enabled = form != null
			) {
				Text(if (form?.public != false) "Rendre privé" else "Rendre public")
			}

			TextButton(
				{ client.forms.edit(ref, open = !form!!.open).orReport(failure) },
				enabled = form != null,
			) {
				Text(if (form?.open != false) "Archiver" else "Désarchiver")
			}
		}

		DisplayError(slice)
		DisplayError(failure)
	}
}
