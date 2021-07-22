package formulaide.ui

import formulaide.api.data.Composite
import formulaide.api.data.Form
import formulaide.api.users.Service
import formulaide.api.users.User
import formulaide.client.Client
import formulaide.client.refreshToken
import formulaide.client.routes.*
import formulaide.ui.components.styledCard
import formulaide.ui.components2.useAsync
import formulaide.ui.utils.GlobalState
import formulaide.ui.utils.text
import formulaide.ui.utils.useGlobalState
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import react.*
import react.dom.p

internal var inProduction = true
internal val defaultClient
	get() = when (inProduction) {
		true -> Client.Anonymous.connect(window.location.protocol + "//" + window.location.host)
		false -> Client.Anonymous.connect("http://localhost:8000")
	}

//region Global state

private val client = GlobalState<Client>(defaultClient)
	.apply {
		subscribers.add { println("The client has been updated") }
		subscribers.add { user.value = null }
	}

fun RBuilder.useClient() = useGlobalState(client)

private val user = GlobalState<User?>(null)
	.apply { subscribers.add { println("The user has been updated: $it") } }

fun RBuilder.useUser() = useGlobalState(user)

private val composites = GlobalState(emptyList<Composite>())
	.apply { subscribers.add { println("The composites have been updated: ${it.size} are stored") } }

fun RBuilder.useComposites() = useGlobalState(composites)
private val compositesRefreshCounter = GlobalState(0)
fun refreshComposites() = compositesRefreshCounter.value++

private val forms = GlobalState(emptyList<Form>())
	.apply { subscribers.add { println("The forms have been updated: ${it.size} are stored") } }

fun RBuilder.useForms() = useGlobalState(forms)
private val formsRefreshCounter = GlobalState(0)
fun refreshForms() = formsRefreshCounter.value++

private val services = GlobalState(emptyList<Service>())
	.apply { subscribers.add { println("The services have been updated: ${it.size} are stored") } }

fun RBuilder.useServices() = useGlobalState(services)
private val servicesRefreshCounter = GlobalState(0)
fun refreshServices() = servicesRefreshCounter.value++

//endregion

/**
 * The main app screen.
 */
val App = fc<RProps> {
	var client by useClient()
	var user by useUser()
	val scope = useAsync()

	val errors = useErrors()

	//region Refresh the user if necessary
	useEffect(client) {
		scope.reportExceptions {
			(client as? Client.Authenticated)
				?.getMe()
				?.let { user = it }
		}
	}
	//endregion

	//region Global list of composites
	val (_, setComposites) = useComposites()
	val refreshComposites by useGlobalState(compositesRefreshCounter)
	useEffect(client, refreshComposites) {
		(client as? Client.Authenticated)
			?.let { c -> scope.reportExceptions { setComposites(c.listData()) } }
			?: run { setComposites(emptyList()) }
	}
	//endregion

	//region Global list of forms
	val (_, setForms) = useForms()
	val refreshForms by useGlobalState(formsRefreshCounter)
	useEffect(client, user, refreshForms) {
		scope.launch {
			setForms((client as? Client.Authenticated)
				         ?.listAllForms()
				         ?: try {
					         client.listForms()
				         } catch (e: Exception) {
					         console.warn("Couldn't access the list of forms, this client is probably dead. Switching to the test client.")
					         inProduction = false
					         client = defaultClient
					         user = null
					         emptyList()
				         })
		}
	}
	//endregion

	//region Global list of services (only if admin)
	var services by useServices()
	val refreshServices by useGlobalState(servicesRefreshCounter)
	useEffect(client, user, refreshServices) {
		scope.reportExceptions {
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
			scope.reportExceptions {
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
			CoroutineScope(job).reportExceptions {
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

	child(Window)

	for (error in errors) {
		child(ErrorCard) {
			key = error.hashCode().toString()
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
