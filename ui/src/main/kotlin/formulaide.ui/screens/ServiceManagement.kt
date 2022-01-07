package formulaide.ui.screens

import formulaide.client.Client
import formulaide.client.routes.closeService
import formulaide.client.routes.createService
import formulaide.client.routes.reopenService
import formulaide.ui.components.*
import formulaide.ui.components.cards.Card
import formulaide.ui.components.text.LightText
import formulaide.ui.components.text.Text
import formulaide.ui.refreshServices
import formulaide.ui.traceRenders
import formulaide.ui.useClient
import formulaide.ui.useServices
import formulaide.ui.utils.map
import org.w3c.dom.HTMLInputElement
import react.FC
import react.Props
import react.dom.html.InputType
import react.dom.html.ReactHTML.div
import react.useRef
import react.useState

val ServiceList = FC<Props>("ServiceList") {
	traceRenders("ServiceList")

	val (client) = useClient()
	if (client !is Client.Authenticated) {
		Card {
			title = "Liste des services"
			failed = true
			Text { text = "Seul un employé peut modifier les services" }
		}
		return@FC
	}

	val services by useServices()
		.map { services -> services.sortedBy { it.name } }

	Card {
		title = "Services"

		var listClosedServices by useState(false)

		styledField("hide-disabled", "Services désactivés") {
			styledCheckbox("hide-disabled", "Afficher les services désactivés") {
				onChange = { listClosedServices = it.target.checked }
			}
		}

		for (service in services.filter { it.open || listClosedServices }) {
			styledFormField {
				Text { text = service.name }

				if (!service.open)
					LightText { text = " Désactivé" }

				div {
					StyledButton {
						text = if (service.open) "Désactiver" else "Activer"
						action = {
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
