package formulaide.ui.screens

import androidx.compose.runtime.*
import formulaide.ui.components.*
import formulaide.ui.navigation.Screen
import formulaide.ui.navigation.client
import formulaide.ui.utils.rememberRef
import formulaide.ui.utils.rememberState
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
) {
	var showArchived by remember { mutableStateOf(false) }

	val users by rememberState(client, showArchived) { client.users.list(includeClosed = showArchived) }

	Page(
		title = "Utilisateurs",
		header = {
			ChipContainerContainer {
				ChipContainer {
					FilterChip("Archivés", showArchived, onUpdate = { showArchived = it })
				}

				ChipContainer {
					RefreshButton { client.users.cache.expireAll() }
				}
			}

			DisplayError(users)
		}
	) {
		for (user in users.valueOrNull ?: emptyList()) {
			ShowUser(user)
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
