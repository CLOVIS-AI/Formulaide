package formulaide.ui.screens

import formulaide.api.data.CompoundDataField
import formulaide.api.data.Data
import formulaide.api.data.Data.Simple.SimpleDataId.TEXT
import formulaide.api.data.NewCompoundData
import formulaide.api.types.Arity
import formulaide.client.Client
import formulaide.client.routes.createData
import formulaide.ui.ScreenProps
import formulaide.ui.fields.editableField
import formulaide.ui.utils.text
import kotlinx.coroutines.launch
import kotlinx.html.InputType
import kotlinx.html.id
import kotlinx.html.js.onClickFunction
import kotlinx.html.js.onSubmitFunction
import org.w3c.dom.HTMLInputElement
import react.dom.*
import react.functionalComponent
import react.useRef
import react.useState

val CreateData = functionalComponent<ScreenProps> { props ->
	val client = props.client
	require(client is Client.Authenticated)

	div {
		text("Données existantes :")
		ul {
			for (data in props.compounds) {
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
					this.compounds = props.compounds
					this.recursive = false
					this.allowModifications = true
					this.allowCreationOfRecursiveData = true

					this.set = { name, data, min, max, subFields ->
						require(subFields == null) { "Une donnée ne peut pas avoir de sous-champs" }

						replaceField(i, field.copy(
							name = name ?: field.name,
							data = data ?: field.data,
							arity = Arity(
								min ?: field.arity.min,
								max ?: field.arity.max
							)
						))
					}
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
						client.createData(data)
						props.refreshCompounds()
					}
				}
			}
		}
	}
}
