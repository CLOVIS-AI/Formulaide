package formulaide.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import formulaide.ui.components.Page
import formulaide.ui.components.PasswordField
import formulaide.ui.components.TextField
import formulaide.ui.navigation.Screen
import formulaide.ui.utils.Role
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text

val Home: Screen = Screen(
	"Accueil",
	Role.ANONYMOUS,
	"?home",
	icon = "ri-home-line",
	iconSelected = "ri-home-fill",
) {
	Page(
		"Formulaide",
	) {
		P {
			Text("Identifiez-vous pour avoir accès à l'espace employé :")
		}

		var email by remember { mutableStateOf("") }
		var password by remember { mutableStateOf("") }

		TextField("Adresse mail", email, onChange = { email = it })
		PasswordField("Mot de passe", password, onChange = { password = it })
	}
}
