package formulaide.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import formulaide.ui.components.*
import formulaide.ui.navigation.Screen
import formulaide.ui.utils.Role
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text

val FormList: Screen = Screen(
	title = "Formulaires",
	requiredRole = Role.ANONYMOUS,
	"?forms",
	icon = "ri-survey-line",
	iconSelected = "ri-survey-fill"
) {
	var showArchived by remember { mutableStateOf(false) }
	var showPrivate by remember { mutableStateOf(true) }
	var showPublic by remember { mutableStateOf(true) }

	Page(
		"Formulaires",
		header = {
			ChipContainer {
				FilterChip("Publics", showPublic, onUpdate = { showPublic = it })
				FilterChip("Privés", showPrivate, onUpdate = { showPrivate = it })
				FilterChip("Archivés", showArchived, onUpdate = { showArchived = it })

				ChipContainerSeparator()

				RefreshButton { /* does nothing for now */ }
			}
		}
	) {
		//TODO in future commits of this MR
		P {
			Text("The list of forms will be here.")
		}
	}
}
