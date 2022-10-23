package formulaide.ui.screens

import androidx.compose.runtime.*
import formulaide.ui.components.*
import formulaide.ui.navigation.Screen
import formulaide.ui.navigation.client
import formulaide.ui.utils.rememberEmptyState
import formulaide.ui.utils.rememberRef
import formulaide.ui.utils.role
import formulaide.ui.utils.user
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import opensavvy.formulaide.core.User
import opensavvy.state.Slice.Companion.valueOrNull
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
		when (client.role) {
			User.Role.ANONYMOUS -> LoginPage()
			else -> HomePage()
		}
	}
}

@Composable
private fun LoginPage() {
	P {
		Text("Identifiez-vous pour avoir accès à l'espace employé :")
	}

	var email by remember { mutableStateOf("") }
	var password by remember { mutableStateOf("") }

	var result by rememberEmptyState()

	TextField("Adresse mail", email, onChange = { email = it })
	PasswordField("Mot de passe", password, onChange = { password = it })

	ButtonContainer {
		MainButton(
			onClick = {
				client.users.logIn(email, password)
					.onEach { result = it }
					.collect()
			}
		) {
			Text("Connexion")
			Loading(result)
		}
	}

	DisplayError(result)
}

@Composable
private fun HomePage() {
	val me by rememberRef(client.user)

	val name = me.valueOrNull?.name

	if (name != null) {
		P {
			Text("Bonjour, ${me.valueOrNull?.name}.")
		}

		P {
			when (me.valueOrNull?.role) {
				User.Role.EMPLOYEE -> Text("Vous êtes un employé.")
				User.Role.ADMINISTRATOR -> Text("Vous êtes administrateur.")
				else -> {}
			}
		}
	}

	DisplayError(me)
}
