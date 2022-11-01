package formulaide.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import formulaide.ui.components.*
import formulaide.ui.navigation.Screen
import formulaide.ui.navigation.client
import formulaide.ui.navigation.currentScreen
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import opensavvy.formulaide.core.User
import opensavvy.formulaide.state.mapSuccess
import opensavvy.formulaide.state.onEachSuccess
import opensavvy.state.Slice
import opensavvy.state.Slice.Companion.pending
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

		var progression: Slice<Unit> by remember { mutableStateOf(pending()) }

		ButtonContainer {
			MainButton(onClick = {
				client.departments.create(name)
					.mapSuccess { }
					.onEach { progression = it }
					.onEachSuccess { currentScreen = DepartmentList }
					.collect()
			}) {
				Text("Créer ce département")
			}
		}

		DisplayError(progression)
	}
}
