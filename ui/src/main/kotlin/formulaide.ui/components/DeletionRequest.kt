package formulaide.ui.components

import formulaide.api.data.Record
import formulaide.ui.utils.classes
import react.FC
import react.Props
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.p

internal external interface DeletionRequestProps : Props {
	var delete: Boolean?
	var onFinished: () -> Unit
	var record: Record
}

internal val DeletionRequest = FC<DeletionRequestProps>("DeletionRequest") { props ->
	div {
		classes = "mb-4 bg-orange-200 rounded-lg p-2"

		p {
			classes = "mb-2 mx-1"

			+"Voulez-vous vraiment supprimer cette donnée ? Il ne sera pas possible de revenir en arrière."
		}

		StyledButton {
			text = "Supprimer définitivement"
			action = { TODO("The record deletion has not been implemented yet.") }
			emphasize = false
		}

		StyledButton {
			text = "Annuler"
			action = { props.onFinished() }
			emphasize = true
		}
	}
}
