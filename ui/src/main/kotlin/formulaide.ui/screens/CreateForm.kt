package formulaide.ui.screens

import formulaide.api.data.Action
import formulaide.api.data.Form
import formulaide.api.fields.Field
import formulaide.api.fields.FormRoot
import formulaide.api.fields.ShallowFormField
import formulaide.api.fields.SimpleField
import formulaide.api.types.Arity
import formulaide.api.types.Ref
import formulaide.api.types.Ref.Companion.createRef
import formulaide.api.users.Service
import formulaide.client.Client
import formulaide.client.routes.createForm
import formulaide.ui.*
import formulaide.ui.components.*
import formulaide.ui.fields.FieldEditor
import formulaide.ui.fields.SwitchDirection
import formulaide.ui.utils.remove
import formulaide.ui.utils.replace
import formulaide.ui.utils.switchOrder
import formulaide.ui.utils.text
import kotlinx.html.InputType
import kotlinx.html.js.onChangeFunction
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLSelectElement
import react.*
import react.dom.*

val CreateForm = fc<RProps> {
	traceRenders("CreateForm")

	val (client) = useClient()
	require(client is Client.Authenticated) { "Cette page n'est accessible que pour les utilisateurs connectés" }

	val services = useServices().value.filter { it.open }

	val formName = useRef<HTMLInputElement>()
	val public = useRef<HTMLInputElement>()

	val (fields, updateFields) = useLocalStorage("form-fields", emptyList<ShallowFormField>())
	val (actions, updateActions) = useLocalStorage("form-actions", emptyList<Action>())

	var maxFieldId by useState(fields.map { it.id.toInt() }.maxOrNull()?.plus(1) ?: 0)
	var maxActionId by useState(actions.map { it.id.toInt() }.maxOrNull()?.plus(1) ?: 0)
	val maxActionFieldId = useState(
		actions
			.flatMap { it.fields?.fields ?: emptyList() }
			.map { it.id.toInt() }
			.maxOrNull()
			?.plus(1)
			?: 0
	)

	val lambdas = useLambdas()

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
				form.validate()
				client.createForm(form)
				clearLocalStorage("form-fields")
				clearLocalStorage("form-actions")

				refreshForms()
				navigateTo(Screen.ShowForms)
			}
		},
		"Effacer" to {
			updateFields { emptyList() }
			updateActions { emptyList() }
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
			for ((i, field) in fields.withIndex()) {
				child(FieldEditor) {
					attrs {
						this.field = field
						key = field.id
						this.replace = { it: Field ->
							updateFields { replace(i, it as ShallowFormField) }
						}.memoIn(lambdas, "replace-${field.id}", i)
						this.remove = {
							updateFields { remove(i) }
						}.memoIn(lambdas, "remove-${field.id}", i)
						switch = { direction: SwitchDirection ->
							updateFields { switchOrder(i, direction) }
						}.memoIn(lambdas, "switch-${field.id}", i)

						depth = 0
						fieldNumber = i
					}
				}
			}

			styledButton("Ajouter un champ", action = {
				updateFields {
					this + ShallowFormField.Simple(
						order = size,
						id = (maxFieldId++).toString(),
						name = "",
						simple = SimpleField.Text(Arity.optional())
					)
				}
			})
		}

		styledField("new-form-actions", "Étapes") {
			for ((i, action) in actions.sortedBy { it.order }.withIndex()) {
				div {
					attrs {
						key = action.id
					}
					styledNesting(
						depth = 0, fieldNumber = i,
						onDeletion = { updateActions { remove(i) } },
					) {
						actionName(action,
						           replace = { updateActions { replace(i, it) } })

						actionReviewerSelection(action, services,
						                        replace = {
							                        updateActions { replace(i, it) }
						                        })

						child(ActionFields) {
							attrs {
								this.action = action
								this.replace = { newAction: Action ->
									updateActions { replace(i, newAction) }
								}.memoIn(lambdas, "action-${action.id}-fields", i)
								this.maxFieldId = maxActionFieldId
							}
						}
					}
				}
			}
			styledButton("Ajouter une étape", action = {
				updateActions {
					this + Action(
						id = (maxActionId++).toString(),
						order = size,
						services.getOrNull(0)?.createRef() ?: error("Aucun service n'a été trouvé"),
						name = "",
					)
				}
			})
			if (actions.isEmpty())
				p { styledErrorText("Un formulaire doit avoir au moins une étape.") }
		}
	}
}

private fun RBuilder.actionName(
	action: Action,
	replace: (Action) -> Unit,
) {
	styledField("new-form-action-${action.id}-name", "Nom de l'étape") {
		styledInput(InputType.text, "new-form-action-${action.id}-name", required = true) {
			value = action.name
			onChangeFunction = { event ->
				val target = event.target as HTMLInputElement
				replace(action.copy(name = target.value))
			}
		}
	}
}

private fun RBuilder.actionReviewerSelection(
	action: Action,
	services: List<Service>,
	replace: (Action) -> Unit,
) {
	styledField("new-form-action-${action.id}-select",
	            "Choix du service") {
		styledSelect {
			for (service in services.filter { it.open }) {
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

					replace(action.copy(reviewer = service.createRef()))
				}
			}
		}
	}
}

private external interface ActionFieldProps : RProps {
	var action: Action
	var replace: (Action) -> Unit
	var maxFieldId: StateInstance<Int>
}

private val ActionFields = memo(fc<ActionFieldProps> { props ->
	val action = props.action
	val replace = props.replace
	var maxFieldId by props.maxFieldId
	val root = action.fields ?: FormRoot(emptyList())

	val lambdas = useLambdas()

	styledField("new-form-action-${action.id}-fields", "Champs réservés à l'administration") {
		for ((i, field) in root.fields.withIndex()) {
			child(FieldEditor) {
				attrs {
					this.field = field
					key = field.id
					this.replace = { it: Field ->
						val newFields = root.fields.replace(i, it as ShallowFormField)
						replace(action.copy(fields = FormRoot(newFields)))
					}.memoIn(lambdas, "action-fields-replace-${field.id}", i, root)
					this.remove = {
						val newFields = root.fields.remove(i)
						replace(action.copy(fields = FormRoot(newFields)))
					}.memoIn(lambdas, "action-fields-remove-${field.id}", i, root)
					this.switch = { direction: SwitchDirection ->
						val newFields = root.fields.switchOrder(i, direction)
						replace(action.copy(fields = FormRoot(newFields)))
					}.memoIn(lambdas, "action-fields-switch-${field.id}", i, root)

					depth = 1
					fieldNumber = i
				}
			}
		}

		styledButton("Ajouter un champ", action = {
			val newFields = root.fields + ShallowFormField.Simple(
				(maxFieldId++).toString(),
				root.fields.size,
				"",
				SimpleField.Text(Arity.mandatory()),
			)

			replace(action.copy(fields = FormRoot(newFields)))
		})
	}
})
