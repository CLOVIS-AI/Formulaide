package formulaide.ui.screens

import androidx.compose.runtime.*
import formulaide.core.form.Form
import formulaide.ui.components.*
import formulaide.ui.navigation.Screen
import formulaide.ui.navigation.client
import formulaide.ui.navigation.currentScreen
import formulaide.ui.theme.RailButton
import formulaide.ui.utils.Role
import formulaide.ui.utils.Role.Companion.role
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.dom.Li
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.Ul

val FormList: Screen = Screen(
	title = "Formulaires",
	requiredRole = Role.ANONYMOUS,
	"?forms",
	icon = "ri-survey-line",
	iconSelected = "ri-survey-fill",
	actions = {
		if (client.role >= Role.ADMINISTRATOR) {
			RailButton(
				FormEditor.icon,
				FormEditor.iconSelected,
				"Créer un formulaire",
				selected = false,
			) { currentScreen = FormEditor }
		}
	}
) {
	var showArchived by remember { mutableStateOf(false) }
	var showPrivate by remember { mutableStateOf(true) }
	var showPublic by remember { mutableStateOf(true) }

	var forms by remember { mutableStateOf(emptyList<Form.Ref>()) }
	suspend fun reloadForms() {
		forms = client.forms.all(includeClosed = showArchived)
	}

	val scope = rememberCoroutineScope()

	LaunchedEffect(client, showArchived) {
		reloadForms()
	}

	Page(
		"Formulaires",
		header = {
			ChipContainerContainer {
				if (client.role >= Role.EMPLOYEE) {
					ChipContainer {
						FilterChip("Publics", showPublic, onUpdate = { showPublic = it })
						FilterChip("Privés", showPrivate, onUpdate = { showPrivate = it })

						if (client.role >= Role.ADMINISTRATOR) {
							FilterChip("Archivés", showArchived, onUpdate = { showArchived = it })
						}
					}
				}

				ChipContainer {
					RefreshButton { scope.launch { reloadForms() } }
				}
			}
		}
	) {

		if (forms.isNotEmpty()) Ul {
			for (form in forms) Li {
				Text(form.id)
			}
		} else P {
			Text("Aucun formulaire ne correspond à cette recherche.")
		}
	}
}
