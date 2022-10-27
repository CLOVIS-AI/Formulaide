package formulaide.ui.screens

import androidx.compose.runtime.*
import formulaide.ui.components.*
import formulaide.ui.navigation.Screen
import formulaide.ui.navigation.client
import formulaide.ui.utils.rememberRef
import formulaide.ui.utils.rememberState
import opensavvy.formulaide.core.Department
import opensavvy.formulaide.core.User
import opensavvy.state.Progression
import opensavvy.state.Slice.Companion.valueOrNull
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text

val DepartmentList: Screen = Screen(
	title = "Départements",
	requiredRole = User.Role.ADMINISTRATOR,
	"?departments",
	icon = "ri-building-line",
	iconSelected = "ri-building-fill",
	actions = {

	}
) {
	var showArchived by remember { mutableStateOf(false) }

	val departments by rememberState(client, showArchived) { client.departments.list(includeClosed = showArchived) }

	Page(
		title = "Départements",
		header = {
			ChipContainerContainer {
				ChipContainer {
					FilterChip("Archivés", showArchived, onUpdate = { showArchived = it })
				}

				ChipContainer {
					RefreshButton { client.departments.cache.expireAll() }
				}
			}

			DisplayError(departments)
		}
	) {
		for (department in departments.valueOrNull ?: emptyList()) {
			ShowDepartment(department)
		}
	}
}

@Composable
private fun ShowDepartment(ref: Department.Ref) {
	val slice by rememberRef(ref)
	val department = slice.valueOrNull

	P {
		if (department != null) {
			Text(department.name)
		}

		if (slice.progression !is Progression.Done)
			Loading(slice)
	}

	DisplayError(slice)
}
