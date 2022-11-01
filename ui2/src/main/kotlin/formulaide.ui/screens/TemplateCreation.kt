package formulaide.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import formulaide.ui.components.*
import formulaide.ui.components.editor.FieldEditor
import formulaide.ui.components.editor.MutableField
import formulaide.ui.navigation.Screen
import formulaide.ui.navigation.client
import formulaide.ui.navigation.currentScreen
import formulaide.ui.utils.rememberPossibleFailure
import formulaide.ui.utils.runOrReport
import kotlinx.datetime.Clock
import opensavvy.formulaide.core.Template
import opensavvy.formulaide.core.User
import org.jetbrains.compose.web.dom.Text

val TemplateCreator: Screen = Screen(
	"Nouveau modèle",
	requiredRole = User.Role.ADMINISTRATOR,
	"?new-template",
	"ri-add-line",
	"ri-add-fill",
	parent = TemplateList,
) {
	Page(
		"Nouveau modèle",
	) {
		var name by remember { mutableStateOf("") }
		TextField("Nom", name, onChange = { name = it })

		var versionName by remember { mutableStateOf("Première version") }
		TextField("Titre de la première version", versionName, onChange = { versionName = it })

		var field: MutableField by remember { mutableStateOf(MutableField.Group("Racine", emptyList(), null)) }
		Paragraph("Champs") {
			FieldEditor(field, onReplace = { field = it })
		}

		val failure = rememberPossibleFailure()

		ButtonContainer {
			MainButton(onClick = {
				runOrReport(failure) {
					val version = Template.Version(Clock.System.now(), versionName, field.toField())
					client.templates.create(name, version).bind()
					currentScreen = TemplateList
				}
			}) {
				Text("Créer ce modèle")
			}
		}

		DisplayError(failure)
	}
}
