package formulaide.ui.screens

import androidx.compose.runtime.*
import formulaide.ui.components.*
import formulaide.ui.navigation.Screen
import formulaide.ui.navigation.client
import formulaide.ui.navigation.currentScreen
import formulaide.ui.theme.RailButton
import formulaide.ui.utils.orReport
import formulaide.ui.utils.rememberPossibleFailure
import formulaide.ui.utils.rememberRef
import formulaide.ui.utils.rememberSlice
import opensavvy.formulaide.core.Department
import opensavvy.formulaide.core.User
import opensavvy.state.slice.valueOrNull
import org.jetbrains.compose.web.dom.Text

val DepartmentList: Screen = Screen(
	title = "Départements",
	requiredRole = User.Role.ADMINISTRATOR,
	"?departments",
	icon = "ri-building-line",
	iconSelected = "ri-building-fill",
	actions = {
		RailButton(
			DepartmentCreator.icon,
			DepartmentCreator.iconSelected,
			"Créer un département",
			selected = false,
		) { currentScreen = DepartmentCreator }
	}
) {
	var showArchived by remember { mutableStateOf(false) }

	val (departments, refresh) = rememberSlice(
		client,
		showArchived
	) { client.departments.list(includeClosed = showArchived) }

	Page(
		title = "Départements",
		header = {
			ChipContainerContainer {
				ChipContainer {
					FilterChip("Archivés", showArchived, onUpdate = { showArchived = it })
				}

				ChipContainer {
					RefreshButton {
						client.departments.cache.expireAll()
						refresh()
					}
				}
			}

			DisplayError(departments)
		}
	) {
		for (department in departments?.valueOrNull ?: emptyList()) {
			ShowDepartment(department)
		}
	}
}

@Composable
private fun ShowDepartment(ref: Department.Ref) {
	val slice by rememberRef(ref)
	val department = slice?.valueOrNull

	val failure = rememberPossibleFailure()

	Paragraph(department?.name ?: "") {
		ButtonContainer {
			TextButton(
				{
					if (department!!.open)
						ref.close().orReport(failure)
					else
						ref.open().orReport(failure)
				},
				enabled = department != null,
			) {
				Text(if (department?.open != false) "Fermer" else "Réouvrir")
			}
		}

		DisplayError(slice)
		DisplayError(failure)
	}
}
