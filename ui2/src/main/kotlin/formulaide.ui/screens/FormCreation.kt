package formulaide.ui.screens

import androidx.compose.runtime.*
import formulaide.ui.components.*
import formulaide.ui.components.editor.*
import formulaide.ui.navigation.Screen
import formulaide.ui.navigation.client
import formulaide.ui.navigation.currentScreen
import formulaide.ui.utils.rememberPossibleFailure
import formulaide.ui.utils.rememberSlice
import formulaide.ui.utils.runOrReport
import kotlinx.datetime.Clock
import opensavvy.formulaide.core.Form
import opensavvy.formulaide.core.User
import opensavvy.state.slice.valueOrNull
import org.jetbrains.compose.web.dom.Text

val FormCreator: Screen = Screen(
	"Nouveau formulaire",
	User.Role.ADMINISTRATOR,
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

		var versionName by remember { mutableStateOf("Première version") }
		TextField("Title de la première version", versionName, onChange = { versionName = it })

		var field: MutableField by remember { mutableStateOf(MutableField.Group("Racine", emptyList(), null)) }
		Paragraph("Champs") {
			FieldEditor(field, onReplace = { field = it })

			if (field !is MutableField.Group)
				DisplayError("Dans la majorité des cas, il est recommandé que la racine d'un formulaire soit un groupe.")
		}

		val (departments, _) = rememberSlice(client) { client.departments.list() }
		val steps = remember { mutableStateListOf<MutableStep>() }

		StepsEditor(steps)

		val failure = rememberPossibleFailure()

		ButtonContainer {
			SecondaryButton(onClick = {
				steps += Form.Step(
					steps.maxOfOrNull { it.id }?.plus(1) ?: 1,
					departments?.valueOrNull?.firstOrNull() ?: return@SecondaryButton,
					null,
				).toMutable()
			}) {
				Text("Ajouter une étape de validation")
			}

			MainButton(onClick = {
				runOrReport(failure) {
					val version =
						Form.Version(Clock.System.now(), versionName, field.toField(), steps.map { it.toCore() })
					client.forms.create(name, public = false, version).bind()
					currentScreen = FormList
				}
			}) {
				Text("Créer ce formulaire")
			}
		}

		if (steps.isEmpty())
			DisplayError("Un formulaire doit contenir au moins une étape")

		if (departments?.valueOrNull?.isEmpty() != false)
			DisplayError("Il est nécessaire de créer un département avant de créer un formulaire")

		DisplayError(failure)
	}
}
