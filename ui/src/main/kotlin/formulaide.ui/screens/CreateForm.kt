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
import formulaide.ui.components.cards.FormCard
import formulaide.ui.components.cards.action
import formulaide.ui.components.cards.submit
import formulaide.ui.components.fields.Nesting
import formulaide.ui.components.text.ErrorText
import formulaide.ui.components.text.Text
import formulaide.ui.fields.FieldEditor
import formulaide.ui.fields.SwitchDirection
import formulaide.ui.utils.remove
import formulaide.ui.utils.replace
import formulaide.ui.utils.switchOrder
import react.*
import react.dom.html.InputType
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.li
import react.dom.html.ReactHTML.option
import react.dom.html.ReactHTML.p
import react.dom.html.ReactHTML.ul

fun CreateForm(original: Form?, copy: Boolean) = FC<Props>("CreateForm") {
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
		Text { text = "Chargement des champs…" }
		LoadingSpinner()
		return@FC
	}

	val (title, buttonName) =
		if (original == null || copy) "Créer un formulaire" to "Créer ce formulaire"
		else "Modifier un formulaire" to "Modifier ce formulaire"

	FormCard {
		this.title = title

		submit(buttonName) {
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
		}
		action("Effacer") {
			updateFields { emptyList() }
			updateActions { emptyList() }
		}

		traceRenders("CreateForm Main card (loaded)")

		if (original != null && copy) {
			Text {
				text =
					"Vous êtes en train de copier ce formulaire. Vous allez créer un nouveau formulaire n'ayant aucun lien avec l'ancien. Les dossiers remplis pour le formulaire précédent ne seront pas visible pour celui-ci."
			}
		}
		if (original != null && !copy) {
			Text {
				text =
					"Vous êtes en train de modifier un formulaire. Vous pouvez effectuer n'importe quelle modification, mais le système devra ensuite vérifier si elle est compatible avec les dossiers remplis précédemment."
			}
			ErrorText {
				text =
					" Il est possible que le système refuse certaines modifications. Il est possible que le système autorise des modifications qui amènent à l'inaccessibilité de certaines données."
			}
			Text { text = " Aucune garantie n'est donnée pour les modifications non listées ci-dessous." }

			p {
				className = "pt-2"

				Text { text = "Modifications qui ne peuvent pas causer de problèmes :" }
			}
			ul {
				className = "list-disc"

				li { Text { text = "Renommer le formulaire" } }
				li { Text { text = "Renommer un champ ou une étape" } }
				li { Text { text = "Modifier l'ordre de plusieurs champs" } }
				li { Text { text = "Modifier la valeur par défaut d'un champ" } }
				li { Text { text = "Modifier le service responsable d'une étape" } }
				li { Text { text = "Créer un champ facultatif (si aucun champ n'a été supprimé)" } }
			}

			p {
				className = "pt-2"

				Text { text = "Modifications qui peuvent être refusées :" }
			}
			ul {
				className = "list-disc"

				li {
					Text {
						text = "Modifier les restrictions des champs (obligatoire, facultatif, longueur maximale…)"
					}
				}
				li {
					Text {
						text =
							"Modifier le type d'un champ (dans certains cas, il n'est pas possible d'annuler la modification)"
					}
				}
				li { Text { text = "Créer un champ facultatif (si un champ avait été supprimé précédemment)" } }
			}

			p {
				className = "pt-2"

				Text { text = "Modifications qui peuvent amener à une perte de données :" }
			}
			ul {
				className = "list-disc pb-4"

				li { Text { text = "Modifier le type d'un champ" } }
				li { Text { text = "Supprimer un champ" } }
			}
		}

		styledField("new-form-name", "Nom") {
			styledInput(InputType.text, "new-form-name", required = true) {
				autoFocus = true
				value = formName
				onChange = {
					formName = it.target.value
				}
			}
		}

		styledField("new-form-visibility", "Est-il public ?") {
			styledCheckbox("new-form-visilibity", "Ce formulaire est visible par les administrés") {
				checked = public
				onChange = {
					public = it.target.checked
				}
			}
		}

		traceRenders("CreateForm Fields")
		styledField("new-form-fields", "Champs") {
			for ((i, field) in fields.withIndex()) {
				FieldEditor {
					this.field = field
					key = field.id
					uniqueId = "initial:${field.id}"

					replace = { it: Field ->
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

			StyledButton {
				text = "Ajouter un champ"
				action = {
					updateFields {
						this + ShallowFormField.Simple(
							order = size,
							id = maxFieldId.toString(),
							name = "",
							simple = SimpleField.Text(Arity.optional())
						)
					}
				}
			}
		}

		traceRenders("CreateForm Actions")
		styledField("new-form-actions", "Étapes") {
			for ((i, action) in actions.sortedBy { it.order }.withIndex()) {
				div {
					key = action.id

					Nesting {
						depth = 0
						fieldNumber = i
						onDeletion = { updateActions { remove(i) } }

						actionName(action,
						           replace = { updateActions { replace(i, it) } })

						actionReviewerSelection(action, services,
						                        replace = {
							                        updateActions { replace(i, it) }
						                        })

						ActionFields {
							this.action = action
							this.replace = { newAction: Action ->
								updateActions { replace(i, newAction) }
							}.memoIn(lambdas, "action-${action.id}-fields", i)
							this.maxFieldId = maxActionFieldId
						}
					}
				}
			}
			StyledButton {
				text = "Ajouter une étape"
				action = {
					updateActions {
						this + Action(
							id = maxActionId.toString(),
							order = size,
							services.getOrNull(0)?.createRef()
								?: error("Aucun service n'a été trouvé"),
							name = "",
						)
					}
				}
			}
			if (actions.isEmpty())
				ErrorText { text = "Un formulaire doit avoir au moins une étape." }
		}
	}

	traceRenders("CreateForm … done")
}

private fun ChildrenBuilder.actionName(
	action: Action,
	replace: (Action) -> Unit,
) {
	styledField("new-form-action-${action.id}-name", "Nom de l'étape") {
		styledInput(InputType.text, "new-form-action-${action.id}-name", required = true) {
			value = action.name
			onChange = { event ->
				val target = event.target
				replace(action.copy(name = target.value))
			}
		}
	}
}

private fun ChildrenBuilder.actionReviewerSelection(
	action: Action,
	services: List<Service>,
	replace: (Action) -> Unit,
) {
	styledField("new-form-action-${action.id}-select",
	            "Choix du service") {
		styledSelect {
			for (service in services.filter { it.open }) {
				option {
					Text { text = service.name }

					value = service.id
					selected = action.reviewer.id == service.id
				}
			}

			onChange = { event ->
				val serviceId = event.target.value
				val service = services.find { it.id == serviceId }
					?: error("Impossible de trouver le service '$serviceId'")

				replace(action.copy(reviewer = service.createRef()))
			}
		}
	}
}

private external interface ActionFieldProps : Props {
	var action: Action
	var replace: (Action) -> Unit
	var maxFieldId: Int
}

private val ActionFields = memo(FC<ActionFieldProps>("ActionFields") { props ->
	traceRenders("ActionFields")

	val action = props.action
	val replace = props.replace
	val maxFieldId = props.maxFieldId
	val root = action.fields ?: FormRoot(emptyList())

	val lambdas = useLambdas()

	styledField("new-form-action-${action.id}-fields", "Champs réservés à l'administration") {
		for ((i, field) in root.fields.withIndex()) {
			FieldEditor {
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

		StyledButton {
			text = "Ajouter un champ"
			this.action = {
				val newFields = root.fields + ShallowFormField.Simple(
					maxFieldId.toString(),
					root.fields.size,
					"",
					SimpleField.Text(Arity.mandatory()),
				)

				replace(action.copy(fields = FormRoot(newFields)))
			}
		}
	}
})
