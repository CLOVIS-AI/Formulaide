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
val App = fc<RProps> {
	var client by useState<Client>(defaultClient)
	var user by useState<User?>(null)
	val scope = useMemo { MainScope() }

	val (errors, setErrors) = useState(emptyList<Throwable>())
	val addError = { error: Throwable -> setErrors(errors + error) }

	//region Refresh the user if necessary
	useEffect(client) {
		launchAndReportExceptions(addError, scope) {
			(client as? Client.Authenticated)
				?.getMe()
				?.let { user = it }

			console.log("Reloaded user")
		}
	}
	//endregion

	//region Global list of composites
	var composites by useState<List<Composite>>(emptyList())
	var refreshComposites by useState(0)
	useEffect(client, refreshComposites) {
		(client as? Client.Authenticated)
			?.let { c -> launchAndReportExceptions(addError, scope) { composites = c.listData() } }
			?: run { composites = emptyList() }

		console.log("Loaded ${composites.size} composites")
	}
	//endregion

	//region Global list of forms
	var forms by useState<List<Form>>(emptyList())
	var refreshForms by useState(0)
	useEffect(client, user, refreshForms) {
		scope.launch {
			forms = (client as? Client.Authenticated)
				?.listAllForms()
				?: try {
					client.listForms()
				} catch (e: Exception) {
					console.warn("Couldn't access the list of forms, this client is probably dead. Switching to the test client.")
					inProduction = false
					client = defaultClient
					user = null
					emptyList()
				}

			console.log("Loaded ${forms.size} forms")
		}
	}
	//endregion

	//region Global list of services (only if admin)
	var services by useState(emptyList<Service>())
	var refreshServices by useState(0)
	useEffect(client, user, refreshServices) {
		launchAndReportExceptions(addError, scope) {
			services =
				(client as? Client.Authenticated)
					?.let { c ->
						if (user?.administrator == true) c.listAllServices()
						else c.listServices()
					}
					?: emptyList()
		}
		console.log("Loaded ${services.size} services")
	}
	//endregion

	//region Refresh token management

	// If the client is anonymous, try to see if we currently have a refreshToken in a cookie
	// If we do, we can bypass the login step
	useEffect(client) {
		(client as? Client.Anonymous)?.let { c ->
			launchAndReportExceptions(addError, scope) {
				val accessToken = client.refreshToken()

				if (accessToken != null) {
					client = c.authenticate(accessToken)
					console.log("Got an access token from the cookie-stored refresh token (page loading)")
				}
			}
		}
	}

	// If the client is connected, wait a few minutes and refresh the access token, to ensure it never gets out of date
	useEffect(client) {
		val job = Job()

		(client as? Client.Authenticated)?.let {
			launchAndReportExceptions(addError, CoroutineScope(job)) {
				delay(1000L * 60 * 10) // every 10 minutes

				val accessToken = client.refreshToken()
				checkNotNull(accessToken) { "Le serveur a refusé de rafraichir le token d'accès. Une raison possible est que votre mot de passe a été modifié." }

				client = defaultClient.authenticate(accessToken)
				console.log("Got an access token from the cookie-stored refresh token (expiration time was near)")
			}
		}

		cleanup { job.cancel() }
	}

	//endregion

	child(Window) {
		attrs {
			this.client = client
			this.user = user
			this.connect = { client = it; user = null }

			this.scope = scope

			this.composites = composites
			this.refreshComposites = { refreshComposites++ }

			this.forms = forms
			this.refreshForms = { refreshForms++ }

			this.services = services
			this.refreshServices = { refreshServices++ }

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
