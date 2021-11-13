package formulaide.ui.screens

import formulaide.api.data.Action
import formulaide.api.data.Form
import formulaide.api.data.FormMetadata
import formulaide.api.fields.Field
import formulaide.api.fields.FormRoot
import formulaide.api.fields.ShallowFormField
import formulaide.api.fields.SimpleField
import formulaide.api.types.Arity
import formulaide.api.types.Ref
import formulaide.api.types.Ref.Companion.createRef
import formulaide.api.users.Service
import formulaide.client.Client
import formulaide.client.routes.compositesReferencedIn
import formulaide.client.routes.createForm
import formulaide.client.routes.editForm
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

fun CreateForm(original: Form?, copy: Boolean) = fc<Props>("CreateForm") {
	traceRenders("CreateForm")

	val (client) = useClient()
	require(client is Client.Authenticated) { "Cette page n'est accessible que pour les utilisateurs connectés" }

	val services = useServices().value.filter { it.open }

	var formName by useLocalStorage("form-name", "")
	var public by useLocalStorage("form-public", false)

	val (fields, updateFields) = useLocalStorage("form-fields", emptyList<ShallowFormField>())
	val (actions, updateActions) = useLocalStorage("form-actions", emptyList<Action>())

	val maxFieldId = useMemo(fields) { fields.maxOfOrNull { it.id.toInt() }?.plus(1) ?: 0 }
	val maxActionId = useMemo(actions) { actions.maxOfOrNull { it.id.toInt() }?.plus(1) ?: 0 }
	val maxActionFieldId = useMemo(actions) {
		actions.flatMap { it.fields?.fields ?: emptyList() }.maxOfOrNull { it.id.toInt() }
			?.plus(1)
			?: 0
	}

	val composites by useAllComposites()
	var formLoaded by useState(false)

	useAsyncEffectOnce {
		val mentionedComposites =
			if (original != null) client.compositesReferencedIn(original) + composites
			else composites

		if (original != null) {
			original.load(mentionedComposites)
			original.actions.forEach { action ->
				action.reviewer.loadFrom(services)
				action.fields?.load(mentionedComposites)
			}
			formName = original.name
			public = original.public
			updateFields { original.mainFields.fields }
			updateActions { original.actions }
		} else {
			FormRoot(fields).load(mentionedComposites)
			actions.forEach { action ->
				action.reviewer.loadFrom(services)
				action.fields?.load(mentionedComposites)
			}
		}
		formLoaded = true
	}

	val lambdas = useLambdas()

	if (!formLoaded) {
		text("Chargement des champs…")
		loadingSpinner()
		return@fc
	}

	val (title, buttonName) =
		if (original == null || copy) "Créer un formulaire" to "Créer ce formulaire"
		else "Modifier un formulaire" to "Modifier ce formulaire"

	styledFormCard(
		title, null,
		buttonName to {
			val form = Form(
				name = formName,
				id = Ref.SPECIAL_TOKEN_NEW,
				public = public,
				open = true,
				mainFields = FormRoot(fields),
				actions = actions
			)

			launch {
				form.validate()

				if (original == null || copy)
					client.createForm(form)
				else
					client.editForm(FormMetadata(
						original.createRef(),
						public = public,
						mainFields = FormRoot(fields),
						actions = actions,
					))

				clearLocalStorage("form-name")
				clearLocalStorage("form-public")
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
		traceRenders("CreateForm Main card (loaded)")

		if (original != null && copy) {
			text("Vous êtes en train de copier ce formulaire. Vous allez créer un nouveau formulaire n'ayant aucun lien avec l'ancien. Les dossiers remplis pour le formulaire précédent ne seront pas visible pour celui-ci.")
		}
		if (original != null && !copy) {
			text("Vous êtes en train de modifier un formulaire. Vous pouvez effectuer n'importe quelle modification, mais le système devra ensuite vérifier si elle est compatible avec les dossiers remplis précédemment.")
			styledErrorText(" Il est possible que le système refuse certaines modifications. Il est possible que le système autorise des modifications qui amènent à l'inaccessibilité de certaines données.")
			text(" Aucune garantie n'est donnée pour les modifications non listées ci-dessous.")

			p("pt-2") { text("Modifications qui ne peuvent pas causer de problèmes :") }
			ul("list-disc") {
				li { text("Renommer le formulaire") }
				li { text("Renommer un champ ou une étape") }
				li { text("Modifier l'ordre de plusieurs champs") }
				li { text("Modifier la valeur par défaut d'un champ") }
				li { text("Modifier le service responsable d'une étape") }
				li { text("Créer un champ facultatif (si aucun champ n'a été supprimé)") }
			}

			p("pt-2") { text("Modifications qui peuvent être refusées :") }
			ul("list-disc") {
				li { text("Modifier les restrictions des champs (obligatoire, facultatif, longueur maximale…)") }
				li { text("Modifier le type d'un champ (dans certains cas, il n'est pas possible d'annuler la modification)") }
				li { text("Créer un champ facultatif (si un champ avait été supprimé précédemment)") }
			}

			p("pt-2") { text("Modifications qui peuvent amener à une perte de données :") }
			ul("list-disc pb-4") {
				li { text("Modifier le type d'un champ") }
				li { text("Supprimer un champ") }
			}
		}

		styledField("new-form-name", "Nom") {
			styledInput(InputType.text, "new-form-name", required = true) {
				autoFocus = true
				value = formName
				onChangeFunction = {
					formName = (it.target as HTMLInputElement).value
				}
			}
		}

		styledField("new-form-visibility", "Est-il public ?") {
			styledCheckbox("new-form-visilibity", "Ce formulaire est visible par les administrés") {
				checked = public
				onChangeFunction = {
					public = (it.target as HTMLInputElement).checked
				}
			}
		}

		traceRenders("CreateForm Fields")
		styledField("new-form-fields", "Champs") {
			for ((i, field) in fields.withIndex()) {
				child(FieldEditor) {
					attrs {
						this.field = field
						key = field.id
						uniqueId = "initial:${field.id}"
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
						id = maxFieldId.toString(),
						name = "",
						simple = SimpleField.Text(Arity.optional())
					)
				}
			})
		}

		traceRenders("CreateForm Actions")
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
						id = maxActionId.toString(),
						order = size,
						services.getOrNull(0)?.createRef()
							?: error("Aucun service n'a été trouvé"),
						name = "",
					)
				}
			})
			if (actions.isEmpty())
				p { styledErrorText("Un formulaire doit avoir au moins une étape.") }
		}
	}

	traceRenders("CreateForm … done")
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

private external interface ActionFieldProps : Props {
	var action: Action
	var replace: (Action) -> Unit
	var maxFieldId: Int
}

private val ActionFields = memo(fc<ActionFieldProps>("ActionFields") { props ->
	traceRenders("ActionFields")

	val action = props.action
	val replace = props.replace
	val maxFieldId = props.maxFieldId
	val root = action.fields ?: FormRoot(emptyList())

	val lambdas = useLambdas()

	styledField("new-form-action-${action.id}-fields", "Champs réservés à l'administration") {
		for ((i, field) in root.fields.withIndex()) {
			child(FieldEditor) {
				attrs {
					this.field = field
					key = field.id
					uniqueId = "action-${action.id}:${field.id}"
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
				maxFieldId.toString(),
				root.fields.size,
				"",
				SimpleField.Text(Arity.mandatory()),
			)

			replace(action.copy(fields = FormRoot(newFields)))
		})
	}
})
