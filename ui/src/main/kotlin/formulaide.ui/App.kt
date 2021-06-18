package formulaide.ui

import formulaide.api.users.User
import formulaide.client.Client
import formulaide.ui.auth.login
import formulaide.ui.utils.text
import kotlinx.coroutines.MainScope
import kotlinx.html.js.onClickFunction
import react.RProps
import react.dom.attrs
import react.dom.button
import react.dom.div
import react.dom.h1
import react.functionalComponent
import react.useMemo
import react.useState

enum class Screen {
	HOME,
}

/**
 * The main app screen.
 */
val App = functionalComponent<RProps> {
	val defaultClient = useMemo { Client.Anonymous.connect("http://localhost:8000") } //TODO: generify

	val (screen, setScreen) = useState(Screen.HOME)

	val (client, setClient) = useState<Client>(defaultClient)
	val (user, setUser) = useState<User?>(null)
	val (scope, _) = useState(MainScope())

	h1 {
		when (user) {
			null -> text("Formulaide • Accès anonyme")
			else -> text("Formulaide • Bonjour ${user.fullName}")
		}
	}

	when (screen) {
		Screen.HOME -> div {
			if (user == null) {
				login {
					this.client = client
					this.onLogin = { client, user ->
						setClient(client)
						setUser(user)
					}
					this.scope = scope
				}
			} else {
				button {
					text("Se déconnecter")
					attrs {
						onClickFunction = { setClient(defaultClient); setUser(null); setScreen(Screen.HOME) }
					}
				}
			}
		}
	}

}
