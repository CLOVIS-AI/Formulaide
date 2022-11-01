package formulaide.ui.screens

import androidx.compose.runtime.*
import formulaide.ui.components.*
import formulaide.ui.navigation.Screen
import formulaide.ui.navigation.client
import formulaide.ui.navigation.currentScreen
import formulaide.ui.theme.RailButton
import formulaide.ui.utils.*
import opensavvy.formulaide.core.Department
import opensavvy.formulaide.core.User
import opensavvy.state.slice.valueOrNull
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
	val (departments) = rememberSlice(client) { client.departments.list() }

	var showEmployees by remember { mutableStateOf(true) }
	var showArchived by remember { mutableStateOf(false) }
	var enableDepartmentFilters by remember { mutableStateOf(false) }
	val showDepartments = remember { mutableStateListOf<Department.Ref>() }

	val (users, refresh) = rememberSlice(client, showArchived) { client.users.list(includeClosed = showArchived) }

	Page(
		title = "Utilisateurs",
		header = {
			ChipContainerContainer {
				ChipContainer {
					FilterChip("Employés", showEmployees, onUpdate = { showEmployees = it })
					FilterChip("Clôturés", showArchived, onUpdate = { showArchived = it })
					FilterChip("Par département", enableDepartmentFilters, onUpdate = { enableDepartmentFilters = it })

					if (enableDepartmentFilters) {
						for (department in departments?.valueOrNull ?: emptyList()) {
							val slice by rememberRef(department)
							val depName = slice?.valueOrNull?.name
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
					RefreshButton {
						client.users.cache.expireAll()
						refresh()
					}
				}
			}

			DisplayError(users)
		}
	) {
		for (user in users?.valueOrNull ?: emptyList()) {
			val slice by rememberRef(user)
			val userData = slice?.valueOrNull

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
	val user = slice?.valueOrNull

	val departments = user?.departments ?: emptySet()

	Paragraph(user?.name ?: "") {
		val failure = rememberPossibleFailure()

		P { Text(user?.email ?: "") }

		P {
			Text("Départements :")
			val (allDepartments) = rememberSlice(client) { client.departments.list() }
			ChipContainer {
				for (department in allDepartments?.valueOrNull ?: emptyList()) {
					val deptSlice by rememberRef(department)
					val depName = deptSlice?.valueOrNull?.name
					if (depName != null) {
						val enabled = department in departments
						FilterChip(
							depName,
							enabled,
							onUpdate = {
								if (enabled) client.users.edit(ref, departments = departments - department)
									.orReport(failure)
								else client.users.edit(ref, departments = departments + department).orReport(failure)
							})
					}
				}
			}
		}

		var password by remember { mutableStateOf<String?>(null) }
		if (password != null)
			P { Text("Mot de passe à usage unique : $password") }

		ButtonContainer {
			if (client.role >= User.Role.ADMINISTRATOR && ref != client.user) {
				TextButton(onClick = {
					password = client.users.resetPassword(ref).orReport(failure)
				}) {
					Text("Réinitialiser le mot de passe")
				}

				TextButton(onClick = {
					client.users.edit(ref, administrator = !user!!.administrator).orReport(failure)
				}, enabled = user != null) {
					Text(if (user?.administrator != false) "Retirer les privilèges" else "Donner les droits d'administration")
				}

				TextButton(onClick = {
					client.users.edit(ref, open = !user!!.open).orReport(failure)
				}, enabled = user != null) {
					Text(if (user?.open != false) "Clôturer" else "Réactiver")
				}
			}
		}

		DisplayError(slice)
		DisplayError(failure)
	}
}
