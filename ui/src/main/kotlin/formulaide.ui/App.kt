package formulaide.ui

import formulaide.api.users.User
import formulaide.client.Client
import formulaide.ui.Role.Companion.role
import formulaide.ui.auth.login
import formulaide.ui.screens.createData
import formulaide.ui.screens.formList
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

enum class Role {
	ANONYMOUS,
	EMPLOYEE,
	ADMINISTRATOR,
	;

	companion object {
		val User?.role
			get() = when {
				this == null -> ANONYMOUS
				!administrator -> EMPLOYEE
				administrator -> ADMINISTRATOR
				else -> error("Should never happen")
			}
	}
}

enum class Screen(val displayName: String, val requiredRole: Role) {
	HOME("Page d'accueil", Role.ANONYMOUS),
	NEW_DATA("Créer une donnée", Role.ADMINISTRATOR),
	FORM_LIST("Liste des formulaires", Role.ANONYMOUS),
}

/**
 * The main app screen.
 */
val App = functionalComponent<RProps> {
	val defaultClient =
		useMemo { Client.Anonymous.connect("http://localhost:8000") } //TODO: generify

	val (currentScreen, setScreen) = useState(Screen.HOME)

	val (client, setClient) = useState<Client>(defaultClient)
	val (user, setUser) = useState<User?>(null)
	val (scope, _) = useState(MainScope())

	h1 {
		when (user) {
			null -> text("Formulaide • Accès anonyme")
			else -> text("Formulaide • Bonjour ${user.fullName}")
		}
	}

	div {
		val availableScreens = Screen.values()
			.filter { it != currentScreen }
			.filter { it.requiredRole.ordinal <= user.role.ordinal }

		for (screen in availableScreens) {
			button {
				text(screen.displayName)
				attrs {
					onClickFunction = { setScreen(screen) }
				}
			}
		}
	}

	when (currentScreen) {
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
						onClickFunction =
							{ setClient(defaultClient); setUser(null) }
					}
				}
			}
		}
		Screen.NEW_DATA -> createData {
			require(client is Client.Authenticated) { "Il n'est pas possible de créer une donnée sans être connecté (client est anonyme)" }
			this.user = user ?: error("Il n'est pas possible de créer une donnée sans être connecté (pas d'utilisateur)")
			this.scope = scope
			this.client = client
		}
		Screen.FORM_LIST -> formList {
			this.client = client
			this.scope = scope
		}
	}

}
