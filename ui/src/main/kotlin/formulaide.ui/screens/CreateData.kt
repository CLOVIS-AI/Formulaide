package formulaide.ui.screens

import formulaide.api.data.Composite
import formulaide.api.fields.DataField
import formulaide.api.fields.SimpleField
import formulaide.api.types.Arity
import formulaide.api.types.Ref
import formulaide.client.Client
import formulaide.client.routes.createData
import formulaide.ui.*
import formulaide.ui.components.*
import formulaide.ui.fields.FieldEditor
import formulaide.ui.utils.remove
import formulaide.ui.utils.replace
import formulaide.ui.utils.text
import kotlinx.html.InputType
import org.w3c.dom.HTMLInputElement
import react.*

val CreateData = fc<RProps> { _ ->
	traceRenders("CreateData")

	val (client) = useClient()
	if (client !is Client.Authenticated) {
		styledCard("Créer un groupe",
		           failed = true) { text("Seuls les administrateurs peuvent créer un groupe.") }
		return@fc
	}

	val formName = useRef<HTMLInputElement>()
	var fields by useState<List<DataField>>(emptyList())
	val (_, navigateTo) = useNavigation()
	var maxId by useState(0)

	styledFormCard(
		"Créer un groupe",
		"Grouper des données utilisées dans plusieurs formulaires permet de mieux gérer les mises à jours. " +
				"Les données composées sont stockées et unifiées entre les services.",
		"Créer ce groupe" to {
			val data = Composite(
				id = Ref.SPECIAL_TOKEN_NEW,
				name = formName.current?.value ?: error("Ce groupe n'a pas de nom"),
				fields = fields
			)

			launch {
				client.createData(data)
				refreshComposites()
				navigateTo(Screen.ShowData)
			}
		},
	) {
		styledField("new-data-name", "Nom") {
			styledInput(InputType.text, "new-data-name", required = true, ref = formName) {
				autoFocus = true
			}
		}

		styledField("data-fields", "Champs") {
			for ((i, field) in fields.sortedBy { it.order }.withIndex()) {
				child(FieldEditor) {
					attrs {
						this.field = field
						replace = {
							fields = fields.replace(i, it as DataField)
						}
						remove = {
							fields = fields.remove(i)
						}

						depth = 0
						fieldNumber = i
					}
				}
			}

			styledButton("Ajouter un champ", action = {
				fields = fields + DataField.Simple(
					order = fields.size,
					id = (maxId++).toString(),
					name = "Nouveau champ",
					simple = SimpleField.Text(Arity.optional())
				)
			})
		}
	}
}
