package formulaide.ui.screens

import androidx.compose.runtime.*
import formulaide.ui.components.*
import formulaide.ui.navigation.Screen
import formulaide.ui.navigation.client
import formulaide.ui.utils.rememberEmptyState
import formulaide.ui.utils.rememberState
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import opensavvy.backbone.Ref.Companion.request
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
	val meRef by rememberState(client) { client.users.me() }
	val me by rememberState(meRef) { meRef.valueOrNull?.request() ?: flowOf() }

	val name = me.valueOrNull?.name

	if (name != null)
		P {
			Text("Bonjour, ${me.valueOrNull?.name}.")
		}

	DisplayError(meRef)
	DisplayError(me)
}
