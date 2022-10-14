package formulaide.ui.screens

import androidx.compose.runtime.*
import formulaide.api.users.PasswordLogin
import formulaide.client.Client
import formulaide.client.routes.login
import formulaide.core.User
import formulaide.ui.components.*
import formulaide.ui.navigation.Screen
import formulaide.ui.navigation.client
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text

val Home: Screen = Screen(
	"Accueil",
	User.Role.ANONYMOUS,
	"?home",
	icon = "ri-home-line",
	iconSelected = "ri-home-fill",
) {
	Page(
		"Formulaide",
	) {
		when (client) {
			is Client.Anonymous -> LoginPage()
			is Client.Authenticated -> HomePage()
		}
	}
}

@Composable
private fun LoginPage() {
	var error by remember { mutableStateOf<Throwable?>(null) }

	P {
		Text("Identifiez-vous pour avoir accès à l'espace employé :")
	}

	var email by remember { mutableStateOf("") }
	var password by remember { mutableStateOf("") }

	TextField("Adresse mail", email, onChange = { email = it })
	PasswordField("Mot de passe", password, onChange = { password = it })

	ButtonContainer {
		MainButton(
			onClick = {
				try {
					error = null
					val token = client.login(PasswordLogin(password, email))
					client = (client as Client.Anonymous).authenticate(token.token)
				} catch (e: Throwable) {
					error = e
				}
			}
		) {
			Text("Connexion")
		}
	}

	if (error != null)
		DisplayError(error!!)
}

@Composable
private fun HomePage() {
	val client = client as Client.Authenticated

	P {
		Text("Bonjour, ${client.me.fullName}.")
	}
}
