package formulaide.ui.screens

import formulaide.api.data.Composite
import formulaide.api.fields.DataField
import formulaide.api.fields.Field
import formulaide.api.fields.SimpleField
import formulaide.api.types.Arity
import formulaide.api.types.Ref
import formulaide.client.Client
import formulaide.client.routes.createData
import formulaide.ui.*
import formulaide.ui.components.*
import formulaide.ui.fields.FieldEditor
import formulaide.ui.fields.SwitchDirection
import formulaide.ui.utils.remove
import formulaide.ui.utils.replace
import formulaide.ui.utils.switchOrder
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
	val fields = useLocalStorage<List<DataField>>("data-fields", emptyList())
	val (_, navigateTo) = useNavigation()
	var maxId by useState(fields.value.map { it.id.toInt() }.maxOrNull()?.plus(1) ?: 0)

	val lambdas = useLambdas()

	styledFormCard(
		"Créer un groupe",
		"Grouper des données utilisées dans plusieurs formulaires permet de mieux gérer les mises à jours. " +
				"Les données composées sont stockées et unifiées entre les services.",
		"Créer ce groupe" to {
			val data = Composite(
				id = Ref.SPECIAL_TOKEN_NEW,
				name = formName.current?.value ?: error("Ce groupe n'a pas de nom"),
				fields = fields.value
			)

			launch {
				client.createData(data)
				clearLocalStorage("data-fields")
				refreshComposites()
				navigateTo(Screen.ShowData)
			}
		},
		"Effacer" to {
			fields.value = emptyList()
		},
	) {
		styledField("new-data-name", "Nom") {
			styledInput(InputType.text, "new-data-name", required = true, ref = formName) {
				autoFocus = true
			}
		}

		styledField("data-fields", "Champs") {
			for ((i, field) in fields.value.withIndex()) {
				child(FieldEditor) {
					attrs {
						this.field = field
						key = field.id
						replace = { it: Field ->
							fields.update { replace(i, it as DataField) }
						}.memoIn(lambdas, "replace-${field.id}", i)
						remove = {
							fields.update { remove(i) }
						}.memoIn(lambdas, "remove-${field.id}", i)
						switch = { direction: SwitchDirection ->
							fields.update { switchOrder(i, direction) }
						}.memoIn(lambdas, "switch-${field.id}", i)

						depth = 0
						fieldNumber = i
					}
				}
			}

			styledButton("Ajouter un champ", action = {
				fields.update {
					this + DataField.Simple(
						order = this.size,
						id = (maxId++).toString(),
						name = "",
						simple = SimpleField.Text(Arity.optional())
					)
				}
			})
		}
	}
}
