package formulaide.ui.screens

import formulaide.api.data.Form
import formulaide.client.Client.Anonymous
import formulaide.client.Client.Authenticated
import formulaide.client.routes.listAllForms
import formulaide.client.routes.listForms
import formulaide.ui.Screen
import formulaide.ui.ScreenProps
import formulaide.ui.utils.text
import kotlinx.coroutines.launch
import kotlinx.html.js.onClickFunction
import react.dom.attrs
import react.dom.button
import react.dom.li
import react.dom.ul
import react.functionalComponent
import react.useEffect
import react.useState

val FormList = functionalComponent<ScreenProps> { props ->
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
			button {
				text("Saisir")
				attrs {
					onClickFunction = { props.navigateTo(Screen.SubmitForm(form)) }
				}
			}
		}
	}

}
