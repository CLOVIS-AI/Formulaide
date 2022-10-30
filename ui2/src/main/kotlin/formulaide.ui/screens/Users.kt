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
import formulaide.ui.utils.user
import opensavvy.formulaide.core.Department
import opensavvy.formulaide.core.User
import opensavvy.state.Progression
import opensavvy.state.Slice.Companion.valueOrNull
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text

val UserList: Screen = Screen(
	title = "Utilisateurs",
	requiredRole = User.Role.ADMINISTRATOR,
	"?users",
	icon = "ri-user-line",
	iconSelected = "ri-user-fill",
	actions = {
		if (client.context.value.role >= User.Role.ADMINISTRATOR) {
			RailButton(
				UserCreator.icon,
				UserCreator.iconSelected,
				"Créer un utilisateur",
				selected = false,
			) { currentScreen = UserCreator }
		}
	}
) {
	val departments by rememberState(client) { client.departments.list() }

	var showEmployees by remember { mutableStateOf(true) }
	var showArchived by remember { mutableStateOf(false) }
	var enableDepartmentFilters by remember { mutableStateOf(false) }
	val showDepartments = remember { mutableStateListOf<Department.Ref>() }

	val users by rememberState(client, showArchived) { client.users.list(includeClosed = showArchived) }

	Page(
		title = "Utilisateurs",
		header = {
			ChipContainerContainer {
				ChipContainer {
					FilterChip("Employés", showEmployees, onUpdate = { showEmployees = it })
					FilterChip("Archivés", showArchived, onUpdate = { showArchived = it })
					FilterChip("Par département", enableDepartmentFilters, onUpdate = { enableDepartmentFilters = it })

					if (enableDepartmentFilters) {
						for (department in departments.valueOrNull ?: emptyList()) {
							val slice by rememberRef(department)
							val depName = slice.valueOrNull?.name
							if (depName != null) {
								val enabled = department in showDepartments
								FilterChip(
									depName,
									enabled,
									onUpdate = {
										if (enabled) showDepartments.remove(department)
										else showDepartments.add(department)
									})
							}
						}
					}
				}

				ChipContainer {
					RefreshButton { client.users.cache.expireAll() }
				}
			}

			DisplayError(users)
		}
	) {
		for (user in users.valueOrNull ?: emptyList()) {
			val slice by rememberRef(user)
			val userData = slice.valueOrNull

			val userIsAdmin = userData?.administrator ?: false
			val userDepartments = userData?.departments ?: emptySet()

			if (showEmployees || userIsAdmin) {
				if (!enableDepartmentFilters)
					ShowUser(user)
				else if (userDepartments.any { it in showDepartments })
					ShowUser(user)
			}
		}
	}
}

@Composable
private fun ShowUser(ref: User.Ref) {
	val slice by rememberRef(ref)
	val user = slice.valueOrNull

	P {
		if (user != null) {
			Text("${user.name}, ${user.email}")
		}

		if (slice.progression !is Progression.Done)
			Loading(slice)
	}

	DisplayError(slice)
}
