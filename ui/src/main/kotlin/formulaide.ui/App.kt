package formulaide.ui

import formulaide.api.data.Composite
import formulaide.api.data.Form
import formulaide.api.users.Service
import formulaide.api.users.User
import formulaide.client.Client
import formulaide.client.refreshToken
import formulaide.client.routes.*
import formulaide.ui.components.styledCard
import formulaide.ui.utils.text
import kotlinx.browser.window
import kotlinx.coroutines.*
import react.*
import react.dom.p

internal var inProduction = true
internal val defaultClient
	get() = when (inProduction) {
		true -> Client.Anonymous.connect(window.location.protocol + "//" + window.location.host)
		false -> Client.Anonymous.connect("http://localhost:8000")
	}

/**
 * The main app screen.
 */
val App = functionalComponent<RProps> {
	val (client, setClient) = useState<Client>(defaultClient)
	val (user, setUser) = useState<User?>(null)
	val (scope, _) = useState(MainScope())

	val (errors, setErrors) = useState(emptyList<Throwable>())
	val addError = { error: Throwable -> setErrors(errors + error) }

	//region Refresh the user if necessary
	useEffect(client) {
		launchAndReportExceptions(addError, scope) {
			if (client is Client.Authenticated)
				setUser(client.getMe())
		}
	}
	//endregion

	//region Global list of compounds
	val (composites, setComposites) = useState<List<Composite>>(emptyList())
	val (refreshComposites, setRefreshComposites) = useState(0)
	useEffect(client, user, refreshComposites) {
		if (client is Client.Authenticated) {
			launchAndReportExceptions(addError, scope) { setComposites(client.listData()) }
		} else setComposites(emptyList())
	}
	//endregion

	//region Global list of forms
	val (forms, setForms) = useState<List<Form>>(emptyList())
	val (refreshForms, setRefreshForms) = useState(0)
	useEffect(client, user, refreshForms) {
		scope.launch {
			setForms(
				if (client is Client.Authenticated) client.listAllForms()
				else
					try {
						client.listForms()
					} catch (e: Exception) {
						console.warn("Couldn't access the list of forms, this client is probably dead. Switching to the test client.")
						inProduction = false
						setClient(defaultClient)
						setUser(null)
						emptyList()
					}
			)
		}
	}
	//endregion

	//region Global list of services (only if admin)
	val (services, setServices) = useState(emptyList<Service>())
	val (refreshServices, setRefreshServices) = useState(0)
	useEffect(client, user, refreshServices) {
		if (client is Client.Authenticated && user != null) {
			launchAndReportExceptions(addError, scope) {
				setServices(
					if (user.administrator) client.listAllServices()
					else client.listServices()
				)
			}
		} else setServices(emptyList())
	}
	//endregion

	//region Refresh token management

	// If the client is anonymous, try to see if we currently have a refreshToken in a cookie
	// If we do, we can bypass the login step
	useEffect(client) {
		if (client is Client.Anonymous) {
			launchAndReportExceptions(addError, scope) {
				val accessToken = client.refreshToken()

				if (accessToken != null)
					setClient(client.authenticate(accessToken))
			}
		}
	}

	// If the client is connected, wait a few minutes and refresh the access token, to ensure it never gets out of date
	useEffect(client) {
		val job = Job()

		if (client is Client.Authenticated) {
			launchAndReportExceptions(addError, CoroutineScope(job)) {
				delay(1000L * 60 * 10) // every 10 minutes

				val accessToken = client.refreshToken()
				checkNotNull(accessToken) { "Le serveur a refusé de rafraichir le token d'accès. Une raison possible est que votre mot de passe a été modifié." }

				setClient(defaultClient.authenticate(accessToken))
			}
		}

		cleanup { job.cancel() }
	}

	//endregion

	child(Window) {
		attrs {
			this.client = client
			this.user = user
			this.connect = { setClient(it); setUser(null) }

			this.scope = scope

			this.composites = composites
			this.refreshComposites = { setRefreshComposites(refreshComposites + 1) }

			this.forms = forms
			this.refreshForms = { setRefreshForms(refreshForms + 1) }

			this.services = services
			this.refreshServices = { setRefreshServices(refreshServices + 1) }

			this.reportError = addError
		}
	}

	for ((i, error) in errors.withIndex()) {
		child(ErrorCard) {
			key = error.hashCode().toString()
			attrs {
				this.scope = scope
				this.error = error
				this.hide = { setErrors(errors.subList(0, i) + errors.subList(i + 1, errors.size)) }
			}
		}
	}

	if (!client.hostUrl.startsWith("https://")) {
		styledCard(
			"Votre accès n'est pas sécurisé",
			"Alerte de sécurité",
			failed = true
		) {
			p { text("Formulaide est connecté à l'API via l'URL ${client.hostUrl}. Cette URL ne commence pas par 'https://'.") }

			p { text("Actuellement, il est possible d'accéder à tout ce que vous faites, dont votre compte et les mots de passes tapés. Veuillez contacter l'administrateur du site.") }
		}
	}
}
