package formulaide.ui

import formulaide.api.users.User
import formulaide.client.Client
import formulaide.ui.auth.login
import formulaide.ui.utils.text
import kotlinx.coroutines.MainScope
import react.RProps
import react.dom.h1
import react.functionalComponent
import react.useState

/**
 * The main app screen.
 */
@JsExport
val App = functionalComponent<RProps> {
	val (client, setClient) = useState<Client>(Client.Anonymous.connect("http://localhost:8000")) //TODO: generify
	val (user, setUser) = useState<User?>(null)
	val (scope, _) = useState(MainScope())

	h1 {
		when (user) {
			null -> text("Formulaide • Accès anonyme")
			else -> text("Formulaide • Bonjour ${user.fullName}")
		}
	}

	login {
		this.client = client
		this.onLogin = { client, user ->
			setClient(client)
			setUser(user)
		}
		this.scope = scope
	}

}
