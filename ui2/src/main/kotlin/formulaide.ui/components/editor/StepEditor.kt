package formulaide.ui.components.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import formulaide.ui.components.ButtonContainer
import formulaide.ui.components.Paragraph
import formulaide.ui.components.TextButton
import formulaide.ui.navigation.client
import formulaide.ui.utils.rememberRef
import formulaide.ui.utils.rememberSlice
import opensavvy.state.slice.valueOrNull
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text

@Composable
fun StepEditor(
	step: MutableStep,
	onDelete: () -> Unit,
) {
	val (departments, _) = rememberSlice(client) { client.departments.list() }

	Paragraph("Étape ${step.id}") {
		P {
			Text("Agents responsables de cette étape :")

			Div {
				for (department in departments?.valueOrNull ?: emptyList()) {
					TextButton(
						{ step.reviewer = department },
						enabled = department != step.reviewer
					) {
						val slice by rememberRef(department)
						Text(slice?.valueOrNull?.name ?: "")
					}
				}
			}
		}

		P {
			Text("Champs remplis par l'agent :")

			if (step.field == null)
				TextButton({ step.field = MutableField.Group("Racine", emptyList(), null) }) { Text("Ajouter") }
			else
				TextButton({ step.field = null }) { Text("Supprimer") }
		}

		if (step.field != null)
			FieldEditor(step.field!!, onReplace = { step.field = it })

		ButtonContainer {
			TextButton({ onDelete() }) { Text("Supprimer cette étape") }
		}
	}
}

@Composable
fun StepsEditor(
	steps: MutableList<MutableStep>,
) {
	for (step in steps) {
		StepEditor(step, onDelete = { steps.remove(step) })
	}
}
