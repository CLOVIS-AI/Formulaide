package formulaide.ui.screens

import androidx.compose.runtime.*
import formulaide.ui.components.*
import formulaide.ui.components.editor.*
import formulaide.ui.components.editor.MutableField.Companion.toMutable
import formulaide.ui.navigation.Screen
import formulaide.ui.navigation.client
import formulaide.ui.navigation.currentScreen
import formulaide.ui.utils.rememberRef
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.onEach
import kotlinx.datetime.Clock
import kotlinx.datetime.toJSDate
import opensavvy.formulaide.core.Form
import opensavvy.formulaide.core.User
import opensavvy.formulaide.state.mapSuccess
import opensavvy.formulaide.state.onEachSuccess
import opensavvy.state.Slice
import opensavvy.state.Slice.Companion.valueOrNull
import opensavvy.state.flatMapSuccess
import opensavvy.state.state
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text

fun FormNewVersion(ref: Form.Ref): Screen = Screen(
	"Nouvelle version d'un formulaire",
	requiredRole = User.Role.ADMINISTRATOR,
	"?new-version-form",
	"ri-add-line",
	"ri-add-fill",
	parent = FormList,
) {
	val slice by rememberRef(ref)
	val form = slice.valueOrNull

	val title = if (form != null)
		"Nouvelle version du formulaire « ${form.name} »"
	else
		"Nouvelle version du formulaire"

	Page(
		title,
		progression = slice.progression,
		header = {
			DisplayError(slice)
		}
	) {
		var versionName by remember { mutableStateOf("") }
		TextField("Titre de la version", versionName, onChange = { versionName = it })

		var field: MutableField by remember { mutableStateOf(MutableField.Group("Racine", emptyList(), null)) }
		val steps = remember { mutableStateListOf<MutableStep>() }

		Paragraph("Charger une version précédente") {
			P {
				Text("Au lieu de partir de zéro, vous pouvez charger une version précédente de ce formulaire comme source de modifications.")
			}

			for (versionRef in form?.versions ?: emptyList()) {
				P {
					val versionSlice by rememberRef(versionRef)
					val version = versionSlice.valueOrNull

					Text("•")
					TextButton(onClick = {
						field = version!!.field.toMutable()
						steps.clear()
						steps.addAll(version.reviewSteps.map { it.toMutable() })
					}, enabled = version != null) {
						if (version != null)
							Text(version.title)
						else
							Loading(versionSlice)
					}
					Text("créée le ${versionRef.version.toJSDate().toLocaleString()}")

					DisplayError(versionSlice)
				}
			}
		}

		Paragraph("Champs") {
			FieldEditor(field, onReplace = { field = it })
		}

		StepsEditor(steps)

		var progression: Slice<Unit> by remember { mutableStateOf(Slice.pending()) }

		ButtonContainer {
			MainButton(onClick = {
				state {
					val version =
						Form.Version(Clock.System.now(), versionName, field.toField(), steps.map { it.toCore() })
					emit(Slice.successful(version))
				}.flatMapSuccess { emitAll(client.forms.createVersion(ref, it)) }
					.mapSuccess { }
					.onEach { progression = it }
					.onEachSuccess { currentScreen = FormList }
					.collect()
			}) {
				Text("Créer cette version")
			}
		}

		DisplayError(progression)
	}
}
