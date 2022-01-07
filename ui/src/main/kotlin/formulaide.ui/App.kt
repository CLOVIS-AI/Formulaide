package formulaide.ui

import formulaide.api.data.Composite
import formulaide.api.data.Form
import formulaide.api.users.Service
import formulaide.client.Client
import formulaide.client.refreshToken
import formulaide.client.routes.*
import formulaide.ui.components.WindowFrame
import formulaide.ui.components.cards.Card
import formulaide.ui.components.text.FooterText
import formulaide.ui.components.text.LightText
import formulaide.ui.components.text.Text
import formulaide.ui.components.useAsync
import formulaide.ui.components.useAsyncEffect
import formulaide.ui.components.useAsyncEffectOnce
import formulaide.ui.screens.clearRecords
import formulaide.ui.utils.*
import io.ktor.client.fetch.*
import kotlinext.js.jso
import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.coroutines.delay
import org.w3c.dom.get
import react.*
import react.dom.html.ReactHTML.br
import react.dom.html.ReactHTML.p

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
		subscribers.add("client console" to { console.log("The client has been updated") })
		subscribers.add("clear records" to { clearRecords() })
	}

fun ChildrenBuilder.useClient(name: String? = null) = useGlobalState(client, name = name)

suspend fun logout() {
	val authenticated = client.value as? Client.Authenticated
		?: error("Impossible de se déconnecter, si on n'est pas connecté")

	authenticated.logout()
	client.value = defaultClient
}

fun ChildrenBuilder.useUser(name: String? = null) = useGlobalState(client, name = name)
	.filterIs<Client.Authenticated>()
	.map { it.me }

private val composites = GlobalState(emptyList<Composite>())
	.apply { subscribers.add("composites console" to { println("The composites have been updated: ${it.size} are stored") }) }
private val compositesDelegate = composites.asDelegated()
	.useListEquality()
	.useEquals()

fun ChildrenBuilder.useComposites() = useGlobalState(composites, compositesDelegate)
	.map { composite -> composite.filter { it.open } }

fun ChildrenBuilder.useAllComposites() = useGlobalState(composites, compositesDelegate)

suspend fun refreshComposites() = (client.value as? Client.Authenticated)
	?.let { c -> compositesDelegate.value = c.listData() }
	?: run { compositesDelegate.value = emptyList() }

private val forms = GlobalState(emptyList<Form>())
	.apply { subscribers.add("forms console" to { println("The forms have been updated: ${it.size} are stored") }) }
private val formsDelegate = forms.asDelegated()
	.useListEquality()
	.useEquals()

fun ChildrenBuilder.useForms() = useGlobalState(forms, formsDelegate)
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
	.apply { subscribers.add("services console" to { println("The services have been updated: ${it.size} are stored") }) }

fun ChildrenBuilder.useServices() = useGlobalState(services)
suspend fun refreshServices() {
	services.value = (
			(client.value as? Client.Authenticated)
				?.let { c ->
					if (c.me.administrator) c.listAllServices()
					else c.listServices()
				}
				?: emptyList())
}

private val bottomText = GlobalState("")

//endregion

/**
 * The main app screen.
 */
val App = FC<Props>("App") {
	//region User fix
	/*
	 * For some reason, without this unused hook, the 'App' component doesn't re-render when:
	 * - Open the app
	 * - Login
	 * - Go to a 'Composites', 'Forms', or 'Services' pages (none appear)
	 * UNLESS the user went to the 'Forms' page _before_ logging in, in which case 'App' renders fine.
	 *
	 * The 'useClient' a few lines below should force a render, but it doesn't.
	 * 'useUser' and 'useClient' are currently implemented using the same hook, so it doesn't make sense.
	 */
	@Suppress("UNUSED_VARIABLE")
	val user by useUser("App user")
	//endregion

	traceRenders("App")

	var client by useClient("App")
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
					formulaide.ui.client.value = c.authenticate(accessToken)
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

	useAsyncEffectOnce {
		bottomText.value = fetch("version.txt").await().text().await()
			.takeIf { "DOCTYPE" !in it } ?: "Les informations de version ne sont pas disponibles."
	}

	Window()

	for (error in errors) {
		ErrorCard {
			key = error.hashCode().toString()
			this.error = error
		}
	}

	if (!client.hostUrl.startsWith("https://")) {
		Card {
			title = "Votre accès n'est pas sécurisé"
			subtitle = "Alerte de sécurité"
			failed = true

			p {
				+"Formulaide est connecté à l'API via l'URL ${client.hostUrl}. Cette URL ne commence pas par 'https://'."
			}

			p {
				+"Actuellement, il est possible d'accéder à tout ce que vous faites, dont votre compte et les mots de passes tapés. Veuillez contacter l'administrateur du site."
			}
		}
	}

	val footerText by useGlobalState(bottomText)
	if (footerText.isNotBlank())
		footerText.split("\n")
			.forEach { br {}; FooterText { text = it } }
}

val StyledAppFrame = FC<Props>("StyledFrame") {
	StrictMode {
		WindowFrame {
			App()
		}
	}
}

//region Crash reporter

private const val errorSectionClass = "mt-2"

val CrashReporter = FC<PropsWithChildren>("CrashReporter") { props ->
	val (boundary, didCatch, error) = useErrorBoundary()

	if (didCatch) {
		Card {
			title = "Erreur"
			subtitle = "Le site a rencontré un échec fatal"
			failed = true

			p {
				Text {
					text =
						"Veuillez signaler cette erreur à l'administrateur, en lui envoyant les informations ci-dessous, à l'adresse incoming+clovis-ai-formulaide-27107472-issue-@incoming.gitlab.com :"
				}
			}

			p {
				className = errorSectionClass

				Text { text = "Ce que j'étais en train de faire : " }
				br {}
				LightText { text = "Ici, expliquez ce que vous étiez en train de faire quand le problème a eu lieu." }
			}

			p {
				className = errorSectionClass

				Text { text = "Error type : " }
				br {}
				LightText { text = "Plantage de l'application, capturé par CrashReporter" }
			}

			p {
				className = errorSectionClass

				Text { text = "Throwable : " }
				error?.stackTraceToString()
					?.removeSurrounding("\n")
					?.split("\n")
					?.forEach {
						br {}
						LightText { text = it.trim() }
					}
					?: LightText { text = "No stacktrace available" }
			}

			for (local in listOf("form-fields", "form-actions", "data-fields")) {
				p {
					className = errorSectionClass

					Text { text = "Local storage : $local" }
					br {}
					LightText { text = window.localStorage[local].toString() }
				}
			}

			p {
				className = errorSectionClass

				Text { text = "Client : " }
				br {}
				LightText { text = client.value.hostUrl }
				br {}
				LightText { text = (client.value as? Client.Authenticated)?.me.toString() }
			}

			for ((globalName, global) in mapOf(
				"Groupes" to composites,
				"Formulaires" to forms,
				"Services" to services,
			)) {
				p {
					className = errorSectionClass

					Text { text = "Cache : $globalName" }
					global.value.forEach {
						br {}
						LightText { text = it.toString() }
					}
				}
			}
		}
	} else {
		child(boundary(
			jso<ErrorBoundaryProps>()
				.apply { children = props.children }
		))
	}
}

//endregion
