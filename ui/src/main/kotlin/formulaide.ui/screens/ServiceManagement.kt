package formulaide.ui.screens

import formulaide.client.Client
import formulaide.client.routes.closeService
import formulaide.client.routes.createService
import formulaide.client.routes.reopenService
import formulaide.ui.components.StyledButton
import formulaide.ui.components.cards.Card
import formulaide.ui.components.cards.FormCard
import formulaide.ui.components.cards.submit
import formulaide.ui.components.inputs.Checkbox
import formulaide.ui.components.inputs.Field
import formulaide.ui.components.inputs.FormField
import formulaide.ui.components.text.LightText
import formulaide.ui.refreshServices
import formulaide.ui.useClient
import formulaide.ui.useServices
import formulaide.ui.utils.map
import react.FC
import react.Props
import react.dom.html.InputType
import react.dom.html.ReactHTML.div
import react.useState
import formulaide.ui.components.inputs.Input as UIInput

val ServiceList = FC<Props>("ServiceList") {
	val (client) = useClient()
	if (client !is Client.Authenticated) {
		Card {
			title = "Liste des services"
			failed = true
			+"Seul un employé peut modifier les services"
		}
		return@FC
	}

	val services by useServices()
		.map { services -> services.sortedBy { it.name } }

	Card {
		title = "Services"

		var listClosedServices by useState(false)

		Field {
			id = "hide-disabled"
			text = "Service désactivés"

			Checkbox {
				id = "hide-disabled"
				text = "Afficher les services désactivés"
				onChange = { listClosedServices = it.target.checked }
			}
		}

		for (service in services.filter { it.open || listClosedServices }) {
			FormField {
				+service.name

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

	var newServiceName by useState("")

	FormCard {
		title = "Créer un service"

		submit("Créer") {
			require(newServiceName.isNotBlank()) { "Le nom d'un service ne peut pas être vide : '$newServiceName'" }

			launch {
				client.createService(newServiceName)
				refreshServices()
				newServiceName = ""
			}
		}

		Field {
			id = "service-name"
			text = "Nom"

			UIInput {
				type = InputType.text
				id = "service-name"
				required = true
				value = newServiceName
				onChange = { newServiceName = it.target.value }
			}
		}
	}
}
