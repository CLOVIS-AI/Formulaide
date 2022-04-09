package formulaide.ui.components

import formulaide.api.data.reportEmailOrDefault
import formulaide.client.Client
import formulaide.ui.*
import formulaide.ui.components.cards.Card
import formulaide.ui.components.text.LightText
import formulaide.ui.utils.*
import kotlinx.browser.window
import kotlinx.js.jso
import org.w3c.dom.get
import react.FC
import react.PropsWithChildren
import react.dom.html.ReactHTML.br
import react.dom.html.ReactHTML.p

private const val errorSectionClass = "mt-2"

val CrashReporter = FC<PropsWithChildren>("CrashReporter") { props ->
	val (boundary, didCatch, error) = useErrorBoundary()
	val config by useConfig()
	val client by useClient()
	val composites by useComposites()
	val forms by useForms()
	val services by useServices()

	if (didCatch) {
		Card {
			title = "Erreur"
			subtitle = "Le site a rencontré un échec fatal"
			failed = true

			p {
				+"Veuillez signaler cette erreur à l'administrateur, en lui envoyant les informations ci-dessous, à l'adresse ${config.reportEmailOrDefault} :"
			}

			p {
				classes = errorSectionClass

				+"Ce que j'étais en train de faire : "
				br {}
				LightText { text = "Ici, expliquez ce que vous étiez en train de faire quand le problème a eu lieu." }
			}

			p {
				classes = errorSectionClass

				+"Error type : "
				br {}
				LightText { text = "Plantage de l'application, capturé par CrashReporter" }
			}

			p {
				classes = errorSectionClass

				+"Throwable : "
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
					classes = errorSectionClass

					+"Local storage : $local"
					br {}
					LightText { text = window.localStorage[local].toString() }
				}
			}

			p {
				classes = errorSectionClass

				+"Client : "
				br {}
				LightText { text = client.hostUrl }
				br {}
				LightText { text = (client as? Client.Authenticated)?.me.toString() }
			}

			for ((globalName, global) in mapOf(
				"Groupes" to composites,
				"Formulaires" to forms,
				"Services" to services,
			)) {
				p {
					classes = errorSectionClass

					+"Cache : $globalName"
					global.forEach {
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
