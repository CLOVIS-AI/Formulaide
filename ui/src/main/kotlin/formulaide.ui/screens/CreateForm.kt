package formulaide.ui.screens

import formulaide.api.data.Action
import formulaide.api.data.Form
import formulaide.api.fields.FormRoot
import formulaide.api.fields.ShallowFormField
import formulaide.api.fields.SimpleField
import formulaide.api.types.Arity
import formulaide.api.types.Ref
import formulaide.api.types.Ref.Companion.createRef
import formulaide.client.Client
import formulaide.client.routes.createForm
import formulaide.ui.*
import formulaide.ui.components.*
import formulaide.ui.fields.FieldEditor
import formulaide.ui.utils.replace
import formulaide.ui.utils.text
import kotlinx.html.InputType
import kotlinx.html.js.onChangeFunction
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLSelectElement
import react.*
import react.dom.attrs
import react.dom.option

val CreateForm = fc<RProps> { _ ->
	traceRenders("CreateForm")

	val (client) = useClient()
	if (client !is Client.Authenticated) {
		styledCard("Créer un formulaire",
		           failed = true) { text("Cette page n'est accessible que pour les utilisateurs connectés") }
		return@fc
	}

	val services = useServices().value.filter { it.open }
	val (_, navigateTo) = useNavigation()

	val formName = useRef<HTMLInputElement>()
	val public = useRef<HTMLInputElement>()

	var fields by useState(emptyList<ShallowFormField>())
	var actions by useState(emptyList<Action>())

	styledFormCard(
		"Créer un formulaire", null,
		"Créer ce formulaire" to {
			val form = Form(
				name = formName.current?.value ?: error("Le formulaire n'a pas de nom"),
				id = Ref.SPECIAL_TOKEN_NEW,
				public = public.current?.checked
					?: error("Le formulaire ne précise pas s'il est public ou interne"),
				open = true,
				mainFields = FormRoot(fields),
				actions = actions
			)

			launch {
				client.createForm(form)

				refreshForms()
				navigateTo(Screen.ShowForms)
			}
		},
		"Ajouter un champ" to {
			fields = fields + ShallowFormField.Simple(
				order = fields.size,
				id = fields.size.toString(),
				name = "Nouveau champ",
				simple = SimpleField.Text(Arity.optional())
			)
		},
		"Ajouter une étape" to {
			actions = actions + Action(
				id = actions.size.toString(),
				order = actions.size,
				services.getOrNull(0)?.createRef() ?: error("Aucun service n'a été trouvé")
			)
		}
	) {
		styledField("new-form-name", "Nom") {
			styledInput(InputType.text, "new-form-name", required = true, ref = formName) {
				autoFocus = true
			}
		}

		styledField("new-form-visibility", "Est-il public ?") {
			styledCheckbox("new-form-visilibity",
			               "Ce formulaire est visible par les administrés",
			               ref = public)
		}

		styledField("new-form-fields", "Champs") {
			styledNesting {
				for ((i, field) in fields.sortedBy { it.order }.withIndex()) {
					child(FieldEditor) {
						attrs {
							this.field = field
							this.replace = {
								fields = fields.replace(i, it as ShallowFormField)
							}

							depth = 0
							fieldNumber = i
						}
					}
				}
			}
		}

		styledField("new-form-actions", "Étapes") {
			styledNesting {
				for ((i, action) in actions.sortedBy { it.order }.withIndex()) {

					styledField("new-form-action-${action.id}-select",
					            "Choix du service") {
						styledSelect {
							for (service in services) {
								option {
									text(service.name)

									attrs {
										value = service.id
										selected = action.reviewer.id == service.id
									}
								}
							}

							attrs {
								onChangeFunction = { event ->
									val serviceId = (event.target as HTMLSelectElement).value
									val service = services.find { it.id == serviceId }
										?: error("Impossible de trouver le service '$serviceId'")

									actions = actions.replace(i,
									                          Action(action.id,
									                                 action.order,
									                                 service.createRef())
									)
								}
							}
						}
					}
				}
			}
		}
	}
}
