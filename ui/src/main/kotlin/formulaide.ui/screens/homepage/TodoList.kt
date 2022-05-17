package formulaide.ui.screens.homepage

import formulaide.api.data.Form
import formulaide.client.Client
import formulaide.client.routes.todoList
import formulaide.ui.components.useAsync
import formulaide.ui.reportExceptions
import formulaide.ui.screens.forms.list.FormDescription
import formulaide.ui.useClient
import formulaide.ui.useForms
import formulaide.ui.utils.DelegatedProperty.Companion.asDelegated
import formulaide.ui.utils.useListEquality
import react.FC
import react.Props
import react.dom.html.ReactHTML
import react.useEffect
import react.useState

val TodoList = FC<Props>("TodoList") {
	val scope = useAsync()
	val allForms by useForms()

	val (client) = useClient()
	if (client !is Client.Authenticated) {
		ReactHTML.p { +"Seuls les utilisateurs connectés peuvent voir la liste des formulaires qui les attendent" }
		return@FC
	}

	var forms by useState(emptyList<Form>())
		.asDelegated()
		.useListEquality()
	var loadingMessage by useState("Chargement des formulaires en cours…")
	if (forms.isEmpty())
		ReactHTML.p { +loadingMessage }

	useEffect(client, allForms) {
		scope.reportExceptions {
			forms = client.todoList().distinct()
			loadingMessage = "Vous n'avez aucun formulaire à vérifier"
		}
	}

	for (form in forms.sortedBy { it.name }) {
		FormDescription {
			this.form = form
		}
	}
}
