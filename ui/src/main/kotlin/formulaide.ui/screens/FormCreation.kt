package formulaide.ui.screens

import formulaide.api.data.Action
import formulaide.api.data.Form
import formulaide.api.data.FormField
import formulaide.client.Client
import formulaide.client.routes.createForm
import formulaide.ui.utils.text
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.html.InputType
import kotlinx.html.id
import kotlinx.html.js.onSubmitFunction
import org.w3c.dom.HTMLInputElement
import react.*
import react.dom.*

external interface FormCreationProps : RProps {
	var client: Client
	var scope: CoroutineScope
}

val CreateForm = functionalComponent<FormCreationProps> { props ->
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
				required = true
				ref = public
			}
		}

		val (fields, setFields) = useState<List<FormField>>(emptyList())

		val (actions, setActions) = useState<List<Action>>(emptyList())

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
					public = public.current?.checked ?: error("Le formulaire ne précise pas s'il est public ou interne"),
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
