package formulaide.ui.components

import formulaide.ui.components.cards.Card
import formulaide.ui.utils.classes
import react.FC
import react.Props
import react.dom.html.ReactHTML.div

data class Notification(
	val title: String,
)

const val NOTIFICATION_LIFETIME_SECONDS: Long = 5

internal external interface NotificationProps : Props {
	var notification: Notification
}

internal val ConfirmationNotification = FC<NotificationProps>("ConfirmationNotification") { props ->
	div {
		classes = "sticky top-0 z-20 flex justify-center w-full"

		Card {
			title = props.notification.title

			+"Votre décision a bien été enregistrée."
		}
	}
}
