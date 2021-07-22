package formulaide.ui.screens

import formulaide.client.Client
import formulaide.client.routes.closeService
import formulaide.client.routes.createService
import formulaide.client.routes.reopenService
import formulaide.ui.*
import formulaide.ui.components.*
import formulaide.ui.components2.useAsync
import formulaide.ui.utils.text
import kotlinx.html.InputType
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onSubmitFunction
import org.w3c.dom.HTMLInputElement
import react.RProps
import react.dom.div
import react.fc
import react.useRef
import react.useState

val ServiceList = fc<RProps> { _ ->
	traceRenders("ServiceList")

	val (client) = useClient()
	if (client !is Client.Authenticated) {
		styledCard("Liste des services",
		           failed = true) { text("Seul un employé peut modifier les services") }
		return@fc
	}

	val services by useServices()
	val scope = useAsync()

	styledCard(
		"Services",
		null
	) {
		var listClosedServices by useState(false)

		styledField("hide-disabled", "Services désactivés") {
			styledCheckbox("hide-disabled", "Afficher les services désactivés") {
				onChangeFunction = { listClosedServices = (it.target as HTMLInputElement).checked }
			}
		}

		for (service in services.filter { it.open || listClosedServices }) {
			styledFormField {
				text(service.name)

				if (!service.open)
					styledLightText(" Désactivé")

				div {

					styledButton(if (service.open) "Désactiver" else "Activer", default = false) {
						scope.reportExceptions {
							if (service.open)
								client.closeService(service)
							else
								client.reopenService(service)
							refreshServices()
						}
					}

				}
			}
		}
	}

	val newServiceName = useRef<HTMLInputElement>()

	styledFormCard(
		"Créer un service",
		null,
		"Créer",
		contents = {
			styledField("service-name", "Nom") {
				styledInput(InputType.text, "service-name", required = true, ref = newServiceName)
			}
		}
	) {
		onSubmitFunction = {
			it.preventDefault()

			scope.reportExceptions {
				val serviceName = newServiceName.current?.value
				requireNotNull(serviceName) { "Le nom d'un service ne peut pas être vide" }
				require(serviceName.isNotBlank()) { "Le nom d'un service ne peut pas être vide : $serviceName" }

				client.createService(serviceName)
				refreshServices()
			}
		}
	}
}
