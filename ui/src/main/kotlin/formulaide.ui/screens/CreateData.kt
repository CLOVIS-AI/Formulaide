package formulaide.ui.screens

import formulaide.api.data.Composite
import formulaide.api.fields.DataField
import formulaide.api.fields.SimpleField
import formulaide.api.types.Arity
import formulaide.api.types.Ref
import formulaide.client.Client
import formulaide.client.routes.createData
import formulaide.ui.ScreenProps
import formulaide.ui.components.styledCard
import formulaide.ui.fields.FieldEditor
import formulaide.ui.utils.replace
import formulaide.ui.utils.text
import kotlinx.coroutines.launch
import kotlinx.html.InputType
import kotlinx.html.id
import kotlinx.html.js.onClickFunction
import kotlinx.html.js.onSubmitFunction
import org.w3c.dom.HTMLInputElement
import react.child
import react.dom.*
import react.functionalComponent
import react.useRef
import react.useState

val CreateData = functionalComponent<ScreenProps> { props ->
	val client = props.client
	require(client is Client.Authenticated)

	styledCard(
		"Données existantes"
	) {
		ul {
			for (data in props.composites) {
				li {
					text(data.name)
				}
			}
		}
	}

	p { text("Une donnée composée permet de combiner plusieurs données possédant un lien (par exemple, le numéro de téléphone et l'adresse mail).") }
	p { text("Les données composées sont stockées et unifiées entre les services.") }

	val formName = useRef<HTMLInputElement>()
	val (fields, setFields) = useState<List<DataField>>(emptyList())

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
				child(FieldEditor) {
					attrs {
						app = props
						this.field = field
						replace = {
							setFields(fields.replace(i, it as DataField))
						}
					}
				}
			}

			br {}
			input(InputType.button, name = "new-data-add-simple") {
				attrs {
					value = "Ajouter un champ"
					onClickFunction = {
						setFields(
							fields + DataField.Simple(
								order = fields.size,
								id = fields.size.toString(),
								name = "Nouveau champ",
								simple = SimpleField.Text(Arity.optional())
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

					val data = Composite(
						id = Ref.SPECIAL_TOKEN_NEW,
						name = formName.current?.value ?: error("Cette donnée n'a pas de nom"),
						fields = fields
					)

					props.scope.launch {
						client.createData(data)
						props.refreshComposites()
					}
				}
			}
		}
	}
}
