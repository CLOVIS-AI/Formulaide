package formulaide.ui.screens.forms.edition

import formulaide.api.data.Action
import formulaide.api.data.Form
import formulaide.api.data.FormMetadata
import formulaide.api.fields.FormRoot
import formulaide.api.fields.ShallowFormField
import formulaide.api.types.Ref
import formulaide.api.types.Ref.Companion.createRef
import formulaide.client.Client
import formulaide.client.routes.compositesReferencedIn
import formulaide.client.routes.createForm
import formulaide.client.routes.editForm
import formulaide.ui.*
import formulaide.ui.components.LoadingSpinner
import formulaide.ui.components.cards.FormCard
import formulaide.ui.components.cards.action
import formulaide.ui.components.cards.submit
import formulaide.ui.components.clearLocalStorage
import formulaide.ui.components.inputs.Checkbox
import formulaide.ui.components.inputs.Field
import formulaide.ui.components.inputs.Input
import formulaide.ui.components.useAsyncEffectOnce
import formulaide.ui.components.useLocalStorage
import react.FC
import react.Props
import react.dom.html.InputType
import react.useState

fun CreateForm(original: Form?, copy: Boolean) = FC<Props>("CreateForm") {
	val (client) = useClient()
	require(client is Client.Authenticated) { "Cette page n'est accessible que pour les utilisateurs connectés" }

	val services = useServices().value.filter { it.open }

	var formName by useLocalStorage("form-name", "")
	var public by useLocalStorage("form-public", false)

	val (fields, updateFields) = useLocalStorage("form-fields", emptyList<ShallowFormField>())
	val (actions, updateActions) = useLocalStorage("form-actions", emptyList<Action>())

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

	if (!formLoaded) {
		+"Chargement des champs…"
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
			+"Vous êtes en train de copier ce formulaire. Vous allez créer un nouveau formulaire n'ayant aucun lien avec l'ancien. Les dossiers remplis pour le formulaire précédent ne seront pas visible pour celui-ci."
		}
		if (original != null && !copy) {
			+"Vous êtes en train de modifier un formulaire. Vous pouvez effectuer n'importe quelle modification, mais le système devra ensuite vérifier si elle est compatible avec les dossiers remplis précédemment."
			FormEditWarning()
		}

		Field {
			id = "new-form-name"
			text = "Nom"

			Input {
				type = InputType.text
				id = "new-form-name"
				required = true
				value = formName
				autoFocus = true
				onChange = { formName = it.target.value }
			}
		}

		Field {
			id = "new-form-visibility"
			text = "Est-il public ?"

			Checkbox {
				id = "new-form-visibility"
				text = "Ce formulaire est visible par les administrés"
				checked = public
				onChange = { public = it.target.checked }
			}
		}

		FormFieldsRenderer {
			this.fields = fields
			this.updateFields = updateFields
		}

		FormActions {
			this.actions = actions
			this.updateActions = updateActions
		}
	}
}
