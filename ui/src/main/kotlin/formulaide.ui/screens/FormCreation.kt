package formulaide.ui.screens

import formulaide.api.data.*
import formulaide.api.data.Data.Simple.SimpleDataId.TEXT
import formulaide.api.types.Arity
import formulaide.api.users.Service
import formulaide.client.Client
import formulaide.client.routes.createForm
import formulaide.client.routes.listData
import formulaide.client.routes.listServices
import formulaide.ui.fields.editableField
import formulaide.ui.utils.text
import kotlinx.coroutines.CoroutineScope
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
				this.set = { name, data, min, max, subFields ->
					val newName = name ?: field.name
					val newData = data ?: field.data
					val newMin = min ?: field.arity.min
					val newMax = max ?: field.arity.max
					val newFields = subFields ?: field.components

					replaceField(
						i,
						field.copy(
							name = newName,
							data = newData,
							arity = Arity(newMin, newMax),
							components = newFields
						)
					)
				}

				this.recursive = true
				this.allowModifications = true
				this.allowCreationOfRecursiveData = false
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
		fun replaceAction(index: Int, value: Action) =
			setActions(actions.subList(0, index) + value + actions.subList(index + 1, actions.size))

		val (services, setServices) = useState(emptyList<Service>())
		useEffect(listOf(props.client)) {
			props.scope.launch {
				val client = props.client
				require(client is Client.Authenticated) { "Impossible de récupérer la liste des services avec un client non connecté" }

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
								val service = services.find { it.id == serviceId } ?: error("Impossible de trouver le service '$serviceId'")

								replaceAction(i, Action.ServiceReview(action.id, action.order, service))
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
					id = 0,
					public = public.current?.checked
						?: error("Le formulaire ne précise pas s'il est public ou interne"),
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
