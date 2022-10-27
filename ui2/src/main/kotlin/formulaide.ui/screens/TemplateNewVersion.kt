package formulaide.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import formulaide.ui.components.*
import formulaide.ui.components.editor.FieldEditor
import formulaide.ui.components.editor.MutableField
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
import opensavvy.formulaide.core.Template
import opensavvy.formulaide.core.User
import opensavvy.formulaide.state.mapSuccess
import opensavvy.formulaide.state.onEachSuccess
import opensavvy.state.Slice
import opensavvy.state.Slice.Companion.pending
import opensavvy.state.Slice.Companion.successful
import opensavvy.state.Slice.Companion.valueOrNull
import opensavvy.state.flatMapSuccess
import opensavvy.state.state
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text

fun TemplateNewVersion(ref: Template.Ref): Screen = Screen(
	"Nouvelle version d'un modèle",
	requiredRole = User.Role.ADMINISTRATOR,
	"?new-version-template",
	"ri-add-line",
	"ri-add-fill",
	parent = TemplateList,
) {
	val slice by rememberRef(ref)
	val template = slice.valueOrNull

	val title = if (template != null)
		"Nouvelle version du modèle « ${template.name} »"
	else
		"Nouvelle version du modèle"

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

		Paragraph("Charger une version précédente") {
			P {
				Text("Au lieu de partir de zéro, vous pouvez charger une version précédente de ce modèle comme source de modifications.")
			}

			for (versionRef in template?.versions ?: emptyList()) {
				P {
					val versionSlice by rememberRef(versionRef)
					val version = versionSlice.valueOrNull

					Text("•")
					TextButton(onClick = { field = version!!.field.toMutable() }, version != null) {
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

		var progression: Slice<Unit> by remember { mutableStateOf(pending()) }

		ButtonContainer {
			MainButton(onClick = {
				state {
					val version = Template.Version(Clock.System.now(), versionName, field.toField())
					emit(successful(version))
				}.flatMapSuccess { emitAll(client.templates.createVersion(ref, it)) }
					.mapSuccess { }
					.onEach { progression = it }
					.onEachSuccess { currentScreen = TemplateList }
					.collect()
			}) {
				Text("Créer cette version")
			}
		}

		DisplayError(progression)
	}
}
