package formulaide.ui.screens

import formulaide.api.data.Composite
import formulaide.api.fields.DataField
import formulaide.api.fields.SimpleField
import formulaide.api.types.Arity
import formulaide.api.types.Ref
import formulaide.client.Client
import formulaide.client.routes.createData
import formulaide.ui.ScreenProps
import formulaide.ui.components.*
import formulaide.ui.fields.FieldEditor
import formulaide.ui.launchAndReportExceptions
import formulaide.ui.reportExceptions
import formulaide.ui.utils.replace
import formulaide.ui.utils.text
import kotlinx.html.InputType
import kotlinx.html.js.onSubmitFunction
import org.w3c.dom.HTMLInputElement
import react.child
import react.dom.li
import react.dom.ul
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

	val formName = useRef<HTMLInputElement>()
	val (fields, setFields) = useState<List<DataField>>(emptyList())

	styledFormCard(
		"Créer une donnée",
		"Une donnée composée permet de combiner plusieurs données possédant un lien " +
				"(par exemple, le numéro de téléphone et l'adresse mail). " +
				"Les données composées sont stockées et unifiées entre les services.",
		"Créer cette donnée",
		"Ajouter un champ" to {
			setFields(
				fields + DataField.Simple(
					order = fields.size,
					id = fields.size.toString(),
					name = "Nouveau champ",
					simple = SimpleField.Text(Arity.optional())
				)
			)
		},
		contents = {
			styledField("new-data-name", "Nom") {
				styledInput(InputType.text, "new-data-name", required = true, ref = formName) {
					autoFocus = true
				}
			}

			styledField("data-fields", "Champs") {
				styledNesting {
					for ((i, field) in fields.sortedBy { it.order }.withIndex()) {
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
				}
			}
		},
	) {
		onSubmitFunction = {
			it.preventDefault()

			val data = reportExceptions(props) {
				Composite(
					id = Ref.SPECIAL_TOKEN_NEW,
					name = formName.current?.value ?: error("Cette donnée n'a pas de nom"),
					fields = fields
				)
			}

			launchAndReportExceptions(props) {
				client.createData(data)
				props.refreshComposites()
			}
		}
	}
}
