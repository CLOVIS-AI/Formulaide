package formulaide.ui.screens

import formulaide.api.data.Form
import formulaide.client.Client
import formulaide.client.Client.Anonymous
import formulaide.client.Client.Authenticated
import formulaide.client.routes.listAllForms
import formulaide.client.routes.listForms
import formulaide.ui.utils.text
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import react.*
import react.dom.li
import react.dom.ul

external interface FormListProps : RProps {
	var client: Client
	var scope: CoroutineScope
}

val FormList = functionalComponent<FormListProps> { props ->
	val (forms, setForms) = useState(emptyList<Form>())
	val (forceUpdate, _) = useState(0)

	useEffect(listOf(props.client, forceUpdate)) {
		props.scope.launch {
			val listForms = when (val client = props.client) {
				is Anonymous -> client.listForms()
				is Authenticated -> client.listAllForms()
			}

			setForms(listForms)
		}
	}

	ul {
		for (form in forms) {
			var text = form.name

			if (!form.public)
				text += " • Ce formulaire est interne"

			li { text(text) }
		}
	}

}

fun RBuilder.formList(handler: FormListProps.() -> Unit) = child(FormList) {
	attrs {
		handler()
	}
}
