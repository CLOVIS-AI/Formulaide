package formulaide.ui

import formulaide.api.data.Composite
import formulaide.api.data.Form
import formulaide.api.users.Service
import formulaide.client.Client
import formulaide.client.refreshToken
import formulaide.client.routes.*
import formulaide.ui.components.styledCard
import formulaide.ui.components.useAsync
import formulaide.ui.components.useAsyncEffect
import formulaide.ui.screens.clearRecords
import formulaide.ui.utils.*
import kotlinx.browser.window
import kotlinx.coroutines.delay
import react.*
import react.dom.p

//region Production / development environments
internal var inProduction = true
internal val defaultClient
	get() = when (inProduction) {
		true -> Client.Anonymous.connect(window.location.protocol + "//" + window.location.host)
		false -> Client.Anonymous.connect("http://localhost:8000")
	}

fun traceRenders(componentName: String) {
	if (!inProduction)
		console.log("Render : $componentName")
}
//endregion

//region Global state

private val client = GlobalState<Client>(defaultClient)
	.apply {
		subscribers.add { println("The client has been updated") }
		subscribers.add { clearRecords() }
	}

fun RBuilder.useClient() = useGlobalState(client)

suspend fun logout() {
	val authenticated = client.value as? Client.Authenticated
		?: error("Impossible de se déconnecter, si on n'est pas connecté")

	authenticated.logout()
	client.value = defaultClient
}

fun RBuilder.useUser() = useGlobalState(client)
	.filterIs<Client.Authenticated>()
	.map { it.me }

private val composites = GlobalState(emptyList<Composite>())
	.apply { subscribers.add { println("The composites have been updated: ${it.size} are stored") } }
private val compositesDelegate = composites.asDelegated()
	.useListEquality()
	.useEquals()

fun RBuilder.useComposites() = useGlobalState(composites, compositesDelegate)
	.map { composite -> composite.filter { it.open } }

fun RBuilder.useAllComposites() = useGlobalState(composites, compositesDelegate)

suspend fun refreshComposites() = (client.value as? Client.Authenticated)
	?.let { c -> compositesDelegate.value = c.listData() }
	?: run { compositesDelegate.value = emptyList() }

private val forms = GlobalState(emptyList<Form>())
	.apply { subscribers.add { println("The forms have been updated: ${it.size} are stored") } }
private val formsDelegate = forms.asDelegated()
	.useListEquality()
	.useEquals()

fun RBuilder.useForms() = useGlobalState(forms, formsDelegate)
suspend fun refreshForms() {
	formsDelegate.value = (client.value as? Client.Authenticated)
		?.listAllForms()
		?: try {
			client.value.listForms()
		} catch (e: Exception) {
			console.warn("Couldn't access the list of forms, this client is probably dead. Switching to the test client.")
			inProduction = false
			client.value = defaultClient
			emptyList()
		}
}

private val services = GlobalState(emptyList<Service>())
	.apply { subscribers.add { println("The services have been updated: ${it.size} are stored") } }

fun RBuilder.useServices() = useGlobalState(services)
suspend fun refreshServices() {
	services.value = (
			(client.value as? Client.Authenticated)
				?.let { c ->
					if (c.me.administrator) c.listAllServices()
					else c.listServices()
				}
				?: emptyList())
}

//endregion

/**
 * The main app screen.
 */
val App = fc<RProps> {
	traceRenders("App")

	var client by useClient()
	val scope = useAsync()

	val errors = useErrors()

	useEffect(client) {
		scope.reportExceptions { refreshComposites() }
		scope.reportExceptions { refreshForms() }
		scope.reportExceptions { refreshServices() }

		// If the client is anonymous, try to see if we currently have a refreshToken in a cookie
		// If we do, we can bypass the login step
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
	useAsyncEffect(client) {
		(client as? Client.Authenticated)?.let {
			reportExceptions {
				delay(1000L * 60 * 25) // every 25 minutes

				val accessToken = client.refreshToken()
				checkNotNull(accessToken) { "Le serveur a refusé de rafraichir le token d'accès. Une raison possible est que votre mot de passe a été modifié." }

				client = defaultClient.authenticate(accessToken)
				console.log("Got an access token from the cookie-stored refresh token (expiration time was near)")
			}
		}
	}

	child(Window)

	for (error in errors) {
		child(ErrorCard) {
			key = error.hashCode().toString()
			attrs {
				this.error = error
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
