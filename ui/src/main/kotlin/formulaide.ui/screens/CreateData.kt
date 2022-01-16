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
import formulaide.ui.components.cards.Card
import formulaide.ui.components.cards.FormCard
import formulaide.ui.components.cards.action
import formulaide.ui.components.cards.submit
import formulaide.ui.components.inputs.Field
import formulaide.ui.components.inputs.Input
import formulaide.ui.fields.editors.FieldEditor
import formulaide.ui.fields.editors.SwitchDirection
import formulaide.ui.utils.remove
import formulaide.ui.utils.replace
import formulaide.ui.utils.switchOrder
import react.*
import react.dom.html.InputType

fun CreateData(original: Composite? = null) = FC<Props>("CreateData") {
	traceRenders("CreateData")

	val (client) = useClient()
	if (client !is Client.Authenticated) {
		Card {
			title = "Créer un groupe"
			failed = true
			+"Seuls les administrateurs peuvent créer un groupe."
		}
		return@FC
	}

	var formName by useLocalStorage("data-name", "")
	val (fields, updateFields) = useLocalStorage<List<DataField>>("data-fields", emptyList())
	val maxId = useMemo(fields) { fields.maxOfOrNull { it.id.toInt() }?.plus(1) ?: 0 }

	// Copy from 'original' if it exists
	useEffect(original) {
		if (original != null) {
			updateFields { original.fields }
			formName = original.name
		}
	}

	val lambdas = useLambdas()

	FormCard {
		title = if (original == null) "Créer un groupe" else "Copier un groupe"

		submit("Créer") {
			val data = Composite(
				id = Ref.SPECIAL_TOKEN_NEW,
				name = formName,
				fields = fields
			)

			launch {
				client.createData(data)
				clearLocalStorage("data-fields")
				clearLocalStorage("data-name")
				refreshComposites()
				navigateTo(Screen.ShowData)
			}
		}
		action("Effacer") { updateFields { emptyList() } }

		Field {
			id = "new-data-name"
			text = "Nom"

			Input {
				type = InputType.text
				id = "new-data-name"
				required = true
				autoFocus = true
				value = formName
				onChange = { formName = it.target.value }
			}
		}

		Field {
			id = "data-fields"
			text = "Champs"

			for ((i, field) in fields.withIndex()) {
				FieldEditor {
					this.field = field
					key = field.id
					uniqueId = "field:${field.id}"

					replace = { it: Field ->
						updateFields { replace(i, it as DataField) }
					}.memoIn(lambdas, "replace-${field.id}", i)

					remove = {
						updateFields { remove(i) }
					}.memoIn(lambdas, "remove-${field.id}", i)

					switch = { direction: SwitchDirection ->
						updateFields { switchOrder(i, direction) }
					}.memoIn(lambdas, "switch-${field.id}", i)

					depth = 0
					fieldNumber = i
				}
			}

			StyledButton {
				text = "Ajouter un champ"
				action = {
					updateFields {
						this + DataField.Simple(
							order = this.size,
							id = maxId.toString(),
							name = "",
							simple = SimpleField.Text(Arity.optional())
						)
					}
				}
			}
		}
	}
}
