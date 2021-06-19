package formulaide.ui.screens

import formulaide.api.data.*
import formulaide.api.data.Data.Simple.SimpleDataId.TEXT
import formulaide.api.types.Arity
import formulaide.client.Client
import formulaide.client.routes.createForm
import formulaide.client.routes.listData
import formulaide.ui.fields.editableField
import formulaide.ui.utils.text
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.html.InputType
import kotlinx.html.id
import kotlinx.html.js.onClickFunction
import kotlinx.html.js.onSubmitFunction
import org.w3c.dom.HTMLInputElement
import react.*
import react.dom.*

external interface FormCreationProps : RProps {
	var client: Client
	var scope: CoroutineScope
}

val CreateForm = functionalComponent<FormCreationProps> { props ->
	val (compoundData, setData) = useState<List<CompoundData>>(emptyList())

	useEffect(listOf(props.client)) {
		props.scope.launch {
			val client = props.client
			require(client is Client.Authenticated) { "Il faut être identifié pour pouvoir créer un formulaire" }

			setData(client.listData())
		}
	}

	form {
		val formName = useRef<HTMLInputElement>()
		label { text("Titre") }
		input(InputType.text, name = "new-form-name") {
			attrs {
				id = "new-form-name"
				required = true
				autoFocus = true
				placeholder = "Nom du formulaire"
				ref = formName
			}
		}

		br {}
		val public = useRef<HTMLInputElement>()
		label { text("Ce formulaire est public (visible par les administrés)") }
		input(InputType.checkBox, name = "new-form-is-public") {
			attrs {
				id = "new-form-is-public"
				ref = public
			}
		}

		val (fields, setFields) = useState<List<FormField>>(emptyList())
		fun replaceField(index: Int, value: FormField) =
			setFields(fields.subList(0, index) + value + fields.subList(index + 1, fields.size))
		for ((i, field) in fields.withIndex()) {
			editableField {
				this.name = field.name
				this.order = field.order
				this.arity = field.arity
				this.data = field.data
				this.compounds = compoundData
				this.set = {  name, data, min, max, subFields ->
					val newName = name ?: field.name
					val newData = data ?: field.data
					val newMin = min ?: field.arity.min
					val newMax = max ?: field.arity.max
					val newFields = subFields ?: field.components

					replaceField(i, field.copy(name = newName, data = newData, arity = Arity(newMin, newMax), components = newFields))
				}

				this.recursive = true
				this.allowModifications = true
			}
		}

		br {}
		input(InputType.button, name = "new-data-add-simple") {
			attrs {
				value = "Ajouter un champ"
				onClickFunction = {
					setFields(
						fields + FormField(
							order = fields.size,
							arity = Arity.optional(),
							id = fields.size,
							name = "Nouveau champ",
							data = Data.simple(TEXT)
						)
					)
				}
			}
		}

		val (actions, setActions) = useState<List<Action>>(emptyList())

		br {}
		input(InputType.submit, name = "new-form-submit") {
			attrs {
				id = "new-form-submit"
				value = "Créer ce formulaire"
			}
		}

		attrs {
			onSubmitFunction = {
				it.preventDefault()

				val form = Form(
					name = formName.current?.value ?: error("Le formulaire n'a pas de nom"),
					id = 0,
					public = public.current?.checked ?: error("Le formulaire ne précise pas s'il est public ou interne"),
					open = true,
					fields = fields,
					actions = actions
				)

				props.scope.launch {
					val client = props.client
					require(client is Client.Authenticated) { "Seuls les administrateurs peuvent créer des formulaires" }
					client.createForm(form)
				}
			}
		}
	}
}

fun RBuilder.createForm(handler: FormCreationProps.() -> Unit) = child(CreateForm) {
	attrs {
		handler()
	}
}
