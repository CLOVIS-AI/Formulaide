package formulaide.ui.screens

import androidx.compose.runtime.*
import formulaide.ui.components.*
import formulaide.ui.navigation.Screen
import formulaide.ui.navigation.client
import formulaide.ui.utils.rememberPossibleFailure
import formulaide.ui.utils.rememberRef
import formulaide.ui.utils.rememberSlice
import formulaide.ui.utils.runOrReport
import opensavvy.formulaide.core.Department
import opensavvy.formulaide.core.User
import opensavvy.state.slice.valueOrNull
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text

val UserCreator: Screen = Screen(
	"Nouvel utilisateur",
	requiredRole = User.Role.ADMINISTRATOR,
	"?new-user",
	"ri-add-line",
	"ri-add-fill",
	parent = UserList,
) {
	Page("Nouvel utilisateur") {
		val (allDepartments) = rememberSlice(client) { client.departments.list() }

		var name by remember { mutableStateOf("") }
		TextField("Nom complet", name, onChange = { name = it })

		var email by remember { mutableStateOf("") }
		TextField("Adresse électronique", email, onChange = { email = it })

		var administrator by remember { mutableStateOf(false) }
		P {
			Text("Rôle")
			FilterChip("Administrateur", administrator, onUpdate = { administrator = it })
		}

		val departments = remember { mutableStateListOf<Department.Ref>() }
		P {
			Text("Départements")
			ChipContainer {
				for (department in allDepartments?.valueOrNull ?: emptyList()) {
					val slice by rememberRef(department)
					val departmentName = slice?.valueOrNull?.name
					if (departmentName != null) {
						val enabled = department in departments
						FilterChip(
							departmentName,
							enabled,
							onUpdate = { if (enabled) departments.remove(department) else departments.add(department) }
						)
					}
				}
			}
		}

		val failure = rememberPossibleFailure()
		var password by remember { mutableStateOf<String?>(null) }

		ButtonContainer {
			MainButton(onClick = {
				runOrReport(failure) {
					val (_, it) = client.users.create(email.trim(), name.trim(), departments.toSet(), administrator)
						.bind()
					password = it
				}
			}) {
				Text("Créer cet utilisateur")
			}
		}

		DisplayError(failure)

		if (password != null) Paragraph("Identifiants") {
			Text("L'utilisateur a bien été créé. Veuillez lui communiquer ses identifiants :")
			P { Text("• Adresse électronique : $email") }
			P { Text("• Mot de passe à usage unique : $password") }
			Text("Lors de sa première connexion, l'utilisateur devra choisir un nouveau mot de passe.")
		}
	}
}
