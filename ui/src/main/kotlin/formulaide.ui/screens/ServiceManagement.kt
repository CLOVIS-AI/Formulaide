package formulaide.ui.screens

import formulaide.client.Client
import formulaide.client.routes.closeService
import formulaide.client.routes.createService
import formulaide.client.routes.reopenService
import formulaide.ui.components.*
import formulaide.ui.refreshServices
import formulaide.ui.traceRenders
import formulaide.ui.useClient
import formulaide.ui.useServices
import formulaide.ui.utils.map
import formulaide.ui.utils.text
import kotlinx.html.InputType
import kotlinx.html.js.onChangeFunction
import org.w3c.dom.HTMLInputElement
import react.Props
import react.dom.div
import react.fc
import react.useRef
import react.useState

val ServiceList = fc<Props>("ServiceList") {
	traceRenders("ServiceList")

	val (client) = useClient()
	if (client !is Client.Authenticated) {
		styledCard("Liste des services",
		           failed = true) { text("Seul un employé peut modifier les services") }
		return@fc
	}

	val services by useServices()
		.map { services -> services.sortedBy { it.name } }

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

	val newServiceName = useRef<HTMLInputElement>()

	styledFormCard(
		"Créer un service",
		null,
		"Créer" to {
			val serviceName = newServiceName.current?.value
			requireNotNull(serviceName) { "Le nom d'un service ne peut pas être vide" }
			require(serviceName.isNotBlank()) { "Le nom d'un service ne peut pas être vide : $serviceName" }

			launch {
				client.createService(serviceName)
				refreshServices()
			}
		},
	) {
		styledField("service-name", "Nom") {
			styledInput(InputType.text, "service-name", required = true, ref = newServiceName)
		}
	}
}
