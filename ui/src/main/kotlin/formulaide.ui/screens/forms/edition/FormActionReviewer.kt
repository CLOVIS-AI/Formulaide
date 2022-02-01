package formulaide.ui.screens.forms.edition

import formulaide.api.types.Ref.Companion.createRef
import formulaide.ui.components.inputs.Field
import formulaide.ui.components.inputs.Select
import formulaide.ui.useServices
import react.FC
import react.dom.html.ReactHTML.option

val FormActionReviewer = FC<FormActionProps>("FormActionReviewer") { props ->
	val action = props.action
	val services by useServices()

	Field {
		id = "new-form-action-${action.id}-select"
		text = "Choix du service"

		Select {
			for (service in services.filter { it.open }) {
				option {
					+service.name

					value = service.id
					selected = action.reviewer.id == service.id
				}
			}

			onChange = { event ->
				val serviceId = event.target.value
				val service = services.find { it.id == serviceId }
					?: error("Impossible de trouver le service '$serviceId'")

				props.onReplace(action.copy(reviewer = service.createRef()))
			}
		}
	}
}
