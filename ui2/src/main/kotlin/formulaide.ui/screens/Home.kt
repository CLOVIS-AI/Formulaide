package formulaide.ui.screens

import androidx.compose.runtime.*
import formulaide.ui.components.*
import formulaide.ui.navigation.Screen
import formulaide.ui.navigation.client
import formulaide.ui.navigation.currentScreen
import formulaide.ui.utils.*
import opensavvy.formulaide.core.User
import opensavvy.state.slice.valueOrNull
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
		val me by rememberRef(client.user)

		when {
			client.role == User.Role.ANONYMOUS -> LoginPage()
			me?.valueOrNull?.forceResetPassword ?: false -> {
				P { Text("Vous vous êtes connectés avec un mot de passe à usage unique. Choisissez un nouveau mot de passe pour pouvoir continuer à utiliser votre compte.") }
				PasswordModificationPage()
			}

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

	val failure = rememberPossibleFailure()

	TextField("Adresse mail", email, onChange = { email = it })
	PasswordField("Mot de passe", password, onChange = { password = it })

	ButtonContainer {
		MainButton(
			onClick = {
				client.users.logIn(email, password).orReport(failure)
			}
		) {
			Text("Connexion")
		}
	}

	DisplayError(failure)
}

@Composable
private fun HomePage() {
	val me by rememberRef(client.user)

	val name = me?.valueOrNull?.name

	if (name != null) {
		P {
			Text("Bonjour, ${me?.valueOrNull?.name}.")
		}

		P {
			when (me?.valueOrNull?.role) {
				User.Role.EMPLOYEE -> Text("Vous êtes un employé.")
				User.Role.ADMINISTRATOR -> Text("Vous êtes administrateur.")
				else -> {}
			}
		}

		if (client.role >= User.Role.EMPLOYEE) {
			SecondaryButton({ currentScreen = PasswordModification }) { Text("Modifier mon mot de passe") }
		}
	}

	DisplayError(me)
}
