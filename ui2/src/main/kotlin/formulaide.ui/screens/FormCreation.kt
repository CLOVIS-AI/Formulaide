package formulaide.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import formulaide.ui.components.*
import formulaide.ui.navigation.Screen
import formulaide.ui.utils.Role
import org.jetbrains.compose.web.css.marginTop
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text

val FormEditor: Screen = Screen(
	"Éditeur de formulaire",
	Role.ADMINISTRATOR,
	"?edit-form",
	"ri-add-line",
	"ri-add-fill",
	parent = FormList,
) {
	Page(
		"Nouveau formulaire",
	) {
		var name by remember { mutableStateOf("") }

		TextField("Nom", name, onChange = { name = it })

		SectionTitle("Champs")
		P { Text("Ces champs seront remplis lors de la saisie du dossier.") }

		var fields by remember { mutableStateOf(emptyList<String>()) }

		//TODO: field editor

		SectionTitle("Étapes de validation")
		P { Text("Choisissez les personnes responsables de la vérification de la validité des saisies.") }
		P {
			Text(
				"""Chaque étape représente le tampon métaphorique d'un membre du département sélectionné.
				|Chaque saisie devra parcourir chaque étape une à une.
				|Une saisie peut être refusée lors de n'importe quelle étape.
			""".trimMargin()
			)
		}

		var steps by remember { mutableStateOf(emptyList<String>()) }

		//TODO action editor

		Div(
			{
				style {
					marginTop(20.px)
				}
			}
		) {
			if (fields.isEmpty())
				DisplayError("Un formulaire doit contenir au moins un champ.")
			else if (steps.isEmpty())
				DisplayError("Un formulaire doit contenir au moins une étape.")
			else
				MainButton(onClick = {}) {
					Text("Créer")
				}
		}
	}
}
