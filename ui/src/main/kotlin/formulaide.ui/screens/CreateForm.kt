package formulaide.ui.screens

import formulaide.api.data.Action
import formulaide.api.data.Form
import formulaide.api.fields.FormField
import formulaide.api.fields.FormRoot
import formulaide.api.fields.SimpleField
import formulaide.api.types.Arity
import formulaide.api.types.Ref
import formulaide.api.users.Service
import formulaide.client.Client
import formulaide.client.routes.createForm
import formulaide.client.routes.listServices
import formulaide.ui.Screen
import formulaide.ui.ScreenProps
import formulaide.ui.fields.FieldEditor
import formulaide.ui.utils.replace
import formulaide.ui.utils.text
import kotlinx.coroutines.launch
import kotlinx.html.InputType
import kotlinx.html.id
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onClickFunction
import kotlinx.html.js.onSubmitFunction
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLSelectElement
import react.*
import react.dom.*

val CreateForm = functionalComponent<ScreenProps> { props ->
	val client = props.client
	require(client is Client.Authenticated)

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

		val (fields, setFields) = useState<List<FormField.Shallow>>(emptyList())
		for ((i, field) in fields.withIndex()) {
			child(FieldEditor) {
				attrs {
					this.app = props
					this.field = field
					this.replace = {
						setFields(fields.replace(i, it as FormField.Shallow))
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
						fields + FormField.Shallow.Simple(
							order = fields.size,
							id = fields.size.toString(),
							name = "Nouveau champ",
							simple = SimpleField.Text(Arity.optional())
						)
					)
				}
			}
		}

		val (actions, setActions) = useState<List<Action>>(emptyList())

		val (services, setServices) = useState(emptyList<Service>())
		useEffect(listOf(props.client)) {
			props.scope.launch {
				setServices(client.listServices())
			}
		}

		for ((i, action) in actions.withIndex()) {
			br {}
			label { text("Action $i") }
			select {
				option {
					text("Vérification par un service")

					attrs {
						value = "SERVICE_REVIEW"
						selected = action is Action.ServiceReview
					}
				}
				option {
					text("Vérification par un employé")

					attrs {
						value = "EMPLOYEE_REVIEW"
						selected = action is Action.EmployeeReview
					}
				}

				attrs {
					onChangeFunction = {
						when ((it.target as HTMLSelectElement).value) {
							"SERVICE_REVIEW" -> Action.ServiceReview(action.id, action.id, services.getOrNull(0) ?: error("Aucun service n'a été trouvé"))
							"EMPLOYEE_REVIEW" -> TODO("La responsabilité d'un employé n'est pas encore implémentée")
						}
					}
				}
			}

			when (action) {
				is Action.ServiceReview -> {
					br {}
					label { text("Choix du service") }
					select {
						for (service in services) {
							option {
								text(service.name)

								attrs {
									value = service.id.toString()
									selected = action.service == service.id
								}
							}
						}

						attrs {
							onChangeFunction = { event ->
								val serviceId = (event.target as HTMLSelectElement).value.toInt()
								val service = services.find { it.id == serviceId }
									?: error("Impossible de trouver le service '$serviceId'")

								setActions(actions.replace(i,
								                           Action.ServiceReview(action.id,
								                                                action.order,
								                                                service)))
							}
						}
					}
				}
				is Action.EmployeeReview -> TODO("La responsabilité d'un employé n'est pas encore implémentée")
			}
		}

		br {}
		input(InputType.button, name = "new-data-add-action") {
			attrs {
				value = "Ajouter une action"
				onClickFunction = {
					setActions(
						actions + Action.ServiceReview(
							id = actions.size,
							order = actions.size,
							service = services.getOrNull(0) ?: error("Aucun service n'a été trouvé")
						)
					)
				}
			}
		}


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
					id = Ref.SPECIAL_TOKEN_NEW,
					public = public.current?.checked
						?: error("Le formulaire ne précise pas s'il est public ou interne"),
					open = true,
					mainFields = FormRoot(fields),
					actions = actions
				)

				props.scope.launch {
					client.createForm(form)

					props.refreshForms()
					props.navigateTo(Screen.ShowForms)
				}
			}
		}
	}
}
