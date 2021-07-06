package formulaide.ui.screens

import formulaide.api.data.Action
import formulaide.api.data.Form
import formulaide.api.fields.FormField
import formulaide.api.fields.FormRoot
import formulaide.api.fields.SimpleField
import formulaide.api.types.Arity
import formulaide.api.types.Ref
import formulaide.client.Client
import formulaide.client.routes.createForm
import formulaide.ui.Screen
import formulaide.ui.ScreenProps
import formulaide.ui.components.*
import formulaide.ui.fields.FieldEditor
import formulaide.ui.utils.replace
import formulaide.ui.utils.text
import kotlinx.coroutines.launch
import kotlinx.html.InputType
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onSubmitFunction
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLSelectElement
import react.child
import react.dom.attrs
import react.dom.option
import react.functionalComponent
import react.useRef
import react.useState

val CreateForm = functionalComponent<ScreenProps> { props ->
	val client = props.client
	require(client is Client.Authenticated)

	val formName = useRef<HTMLInputElement>()
	val public = useRef<HTMLInputElement>()

	val (fields, setFields) = useState<List<FormField.Shallow>>(emptyList())
	val (actions, setActions) = useState<List<Action>>(emptyList())

	val services = props.services.filter { it.open }

	styledFormCard(
		"Créer un formulaire", null,
		"Créer ce formulaire",
		"Ajouter un champ" to {
			setFields(
				fields + FormField.Shallow.Simple(
					order = fields.size,
					id = fields.size.toString(),
					name = "Nouveau champ",
					simple = SimpleField.Text(Arity.optional())
				)
			)
		},
		"Ajouter une étape" to {
			setActions(
				actions + Action.ServiceReview(
					id = actions.size,
					order = actions.size,
					service = services.getOrNull(0) ?: error("Aucun service n'a été trouvé")
				)
			)
		},
		contents = {
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
				}
			}

			styledField("new-form-actions", "Étapes") {
				styledNesting {
					for ((i, action) in actions.withIndex()) {
						styledField("action-$i", "Action $i") {
							styledSelect(
								onSelect = {
									when (it.value) {
										"SERVICE_REVIEW" -> Action.ServiceReview(
											action.id,
											action.id,
											services.getOrNull(0)
												?: error("Aucun service n'a été trouvé"))
										"EMPLOYEE_REVIEW" -> TODO("La responsabilité d'un employé n'est pas encore implémentée")
									}
								}
							) {
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
							}
						}

						when (action) {
							is Action.ServiceReview -> {
								styledField("new-form-action-${action.id}-select",
								            "Choix du service") {
									styledSelect {
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
												val serviceId =
													(event.target as HTMLSelectElement).value.toInt()
												val service = services.find { it.id == serviceId }
													?: error("Impossible de trouver le service '$serviceId'")

												setActions(
													actions.replace(
														i,
														Action.ServiceReview(action.id,
														                     action.order,
														                     service)))
											}
										}
									}
								}
							}
							is Action.EmployeeReview -> TODO("La responsabilité d'un employé n'est pas encore implémentée")
						}
					}
				}
			}
		}
	) {
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
