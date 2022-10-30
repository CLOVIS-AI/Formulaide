package formulaide.ui.screens

import androidx.compose.runtime.*
import formulaide.ui.components.DisplayError
import formulaide.ui.components.MainButton
import formulaide.ui.components.Page
import formulaide.ui.components.PasswordField
import formulaide.ui.navigation.Screen
import formulaide.ui.navigation.client
import kotlinx.coroutines.flow.onEach
import opensavvy.formulaide.core.User
import opensavvy.state.Slice
import opensavvy.state.firstResultOrNull
import org.jetbrains.compose.web.dom.Text

val PasswordModification: Screen = Screen(
	"Modifier mon mot de passe",
	User.Role.EMPLOYEE,
	"?set-password",
	"ri-lock-line",
	"ri-lock-fill",
	parent = Home
) {
	Page("Modification de mot de passe") {
		PasswordModificationPage()
	}
}

@Composable
fun PasswordModificationPage() {
	var oldPassword by remember { mutableStateOf("") }
	PasswordField("Mot de passe actuel", oldPassword, onChange = { oldPassword = it })

	var newPassword by remember { mutableStateOf("") }
	PasswordField("Nouveau mot de passe", newPassword, onChange = { newPassword = it })

	var newPassword2 by remember { mutableStateOf("") }
	PasswordField("Nouveau mot de passe (confirmation)", newPassword2, onChange = { newPassword2 = it })

	var progression: Slice<Unit> by remember { mutableStateOf(Slice.pending()) }

	if (newPassword == newPassword2 && newPassword.isNotBlank())
		MainButton(onClick = {
			client.users.setPassword(oldPassword, newPassword)
				.onEach { progression = it }
				.firstResultOrNull()
		}) {
			Text("Modifier mon mot de passe")
		}

	DisplayError(progression)
}
