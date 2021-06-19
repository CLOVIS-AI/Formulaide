package formulaide.ui.screens

import formulaide.api.data.CompoundData
import formulaide.api.data.CompoundDataField
import formulaide.api.data.Data
import formulaide.api.data.Data.Simple.SimpleDataId.TEXT
import formulaide.api.data.NewCompoundData
import formulaide.api.types.Arity
import formulaide.api.users.User
import formulaide.client.Client
import formulaide.client.routes.createData
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

external interface CreateDataProps : RProps {
	var user: User
	var client: Client.Authenticated
	var scope: CoroutineScope
}

val CreateData = functionalComponent<CreateDataProps> { props ->
	val (existingData, setExistingData) = useState<List<CompoundData>>(emptyList())
	val (creationCounter, setCreationCounter) = useState(0) // to be able to force a download of the list

	useEffect(listOf(props.user, props.client, creationCounter)) {
		props.scope.launch {
			println("Listing the existing data…")
			setExistingData(props.client.listData())
		}
	}

	div {
		text("Données existantes :")
		ul {
			for (data in existingData) {
				li {
					text(data.name)
				}
			}
		}
	}

	p { text("Une donnée composée permet de combiner plusieurs données possédant un lien (par exemple, le numéro de téléphone et l'adresse mail).") }
	p { text("Les données composées sont stockées et unifiées entre les services.") }

	val formName = useRef<HTMLInputElement>()
	val (fields, setFields) = useState<List<CompoundDataField>>(emptyList())

	fun replaceField(index: Int, value: CompoundDataField) =
		setFields(fields.subList(0, index) + value + fields.subList(index + 1, fields.size))

	div {
		text("Créer une donnée :")
		form {
			div {
				label { text("Nom") }
				input(InputType.text, name = "new-data-name") {
					attrs {
						id = "new-data-name"
						required = true
						autoFocus = true
						placeholder = "Nom de la donnée"
						ref = formName
					}
				}
			}

			for ((i, field) in fields.withIndex()) {
				br {}
				editableField {
					this.arity = field.arity
					this.order = field.order
					this.name = field.name
					this.data = field.data
					this.setName = { replaceField(i, field.copy(name = it)) }
					this.setArity = { replaceField(i, field.copy(arity = it)) }
					this.setDataType = { replaceField(i, field.copy(data = it)) }
					this.compounds = existingData
				}
			}

			br {}
			input(InputType.button, name = "new-data-add-simple") {
				attrs {
					value = "Ajouter un champ"
					onClickFunction = {
						setFields(
							fields + CompoundDataField(
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

			br {}
			input(InputType.submit, name = "new-data-submit") {
				attrs {
					id = "new-data-submit"
					value = "Créer cette donnée"
				}
			}

			attrs {
				onSubmitFunction = {
					it.preventDefault()

					val data = NewCompoundData(
						name = formName.current?.value ?: error("Cette donnée n'a pas de nom"),
						fields = fields
					)

					props.scope.launch {
						props.client.createData(data)

						setCreationCounter(creationCounter+1) // Force re-query of the data list
					}
				}
			}
		}
	}
}

fun RBuilder.createData(handler: CreateDataProps.() -> Unit) = child(CreateData) {
	attrs {
		handler()
	}
}
