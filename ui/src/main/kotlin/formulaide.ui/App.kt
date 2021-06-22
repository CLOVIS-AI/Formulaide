package formulaide.ui

import formulaide.api.data.CompoundData
import formulaide.api.data.Form
import formulaide.api.users.User
import formulaide.client.Client
import formulaide.client.routes.getMe
import formulaide.client.routes.listAllForms
import formulaide.client.routes.listData
import formulaide.client.routes.listForms
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import react.*

val defaultClient = Client.Anonymous.connect("http://localhost:8000") //TODO: generify

/**
 * The main app screen.
 */
val App = functionalComponent<RProps> {
	val (client, setClient) = useState<Client>(defaultClient)
	val (user, setUser) = useState<User?>(null)
	val (scope, _) = useState(MainScope())

	//region Refresh the user if necessary
	useEffect(listOf(client)) {
		scope.launch {
			if (client is Client.Authenticated)
				setUser(client.getMe())
		}
	}
	//endregion

	//region Global list of compounds
	val (compounds, setCompounds) = useState<List<CompoundData>>(emptyList())
	val (refreshCompounds, setRefreshCompounds) = useState(0)
	useEffect(listOf(client, user, refreshCompounds)) {
		if (client is Client.Authenticated) {
			scope.launch { setCompounds(client.listData()) }
		} else setCompounds(emptyList())
	}
	//endregion

	//region Global list of forms
	val (forms, setForms) = useState<List<Form>>(emptyList())
	val (refreshForms, setRefreshForms) = useState(0)
	useEffect(listOf(client, user, refreshForms)) {
		scope.launch {
			setForms(
				if (client is Client.Authenticated) client.listAllForms()
				else client.listForms()
			)
		}
	}
	//endregion

	child(Window) {
		attrs {
			this.client = client
			this.user = user
			this.connect = setClient

			this.scope = scope

			this.compounds = compounds
			this.refreshCompounds = { setRefreshCompounds(refreshCompounds + 1) }

			this.forms = forms
			this.refreshForms = { setRefreshForms(refreshForms + 1) }
		}
	}

}
