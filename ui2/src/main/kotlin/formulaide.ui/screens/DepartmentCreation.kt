package formulaide.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import formulaide.ui.components.*
import formulaide.ui.navigation.Screen
import formulaide.ui.navigation.client
import formulaide.ui.navigation.currentScreen
import formulaide.ui.utils.orReport
import formulaide.ui.utils.rememberPossibleFailure
import opensavvy.formulaide.core.User
import org.jetbrains.compose.web.dom.Text

val DepartmentCreator: Screen = Screen(
	"Nouveau département",
	requiredRole = User.Role.ADMINISTRATOR,
	"?new-department",
	"ri-add-line",
	"ri-add-fill",
	parent = DepartmentList
) {
	Page("Nouveau département") {
		var name by remember { mutableStateOf("") }
		TextField("Nom", name, onChange = { name = it })

		val failure = rememberPossibleFailure()

		ButtonContainer {
			MainButton(onClick = {
				client.departments.create(name).orReport(failure)
				currentScreen = DepartmentList
			}) {
				Text("Créer ce département")
			}
		}

		DisplayError(failure)
	}
}
