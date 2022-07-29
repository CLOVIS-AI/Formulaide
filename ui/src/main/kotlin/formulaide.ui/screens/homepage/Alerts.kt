package formulaide.ui.screens.homepage

import formulaide.api.data.Alert
import formulaide.api.types.Ref.Companion.loadIfNecessary
import formulaide.api.users.User
import formulaide.client.Client
import formulaide.client.routes.alerts
import formulaide.client.routes.editUser
import formulaide.client.routes.listUsers
import formulaide.ui.components.LoadingSpinner
import formulaide.ui.components.StyledButton
import formulaide.ui.components.cards.Card
import formulaide.ui.components.cards.action
import formulaide.ui.components.useAsyncEffect
import formulaide.ui.components.useAsyncEffectOnce
import formulaide.ui.useClient
import formulaide.ui.utils.classes
import kotlinx.coroutines.delay
import react.FC
import react.Props
import react.dom.html.ReactHTML.br
import react.dom.html.ReactHTML.p
import react.useEffect
import react.useState
import kotlin.js.Date

val Alerts = FC<Props>("Alerts") {
	var recentOnly by useState(true)
	val (client) = useClient()
	require(client is Client.Authenticated) { "Les utilisateurs non-connectés ne peuvent pas afficher les alertes" }

	var alerts by useState<List<Alert>>()
	val refresh = suspend {
		alerts = client.alerts()
			.sortedBy { it.timestamp }
			.asReversed()
	}

	useAsyncEffectOnce {
		refresh()
	}

	useAsyncEffect(alerts, client) {
		// Refresh every minute
		delay(1000 * 60)
		refresh()
	}

	var selectedAlerts by useState(emptyList<Alert>())
	useEffect(alerts, recentOnly) {
		val now = Date()
		val sevenDaysAgo = Date(now.getFullYear(), now.getMonth(), now.getDate() - 7)

		selectedAlerts = if (recentOnly || alerts == null)
			alerts ?: emptyList()
		else
			alerts!!.filter { it.timestamp > sevenDaysAgo.getTime() }
	}

	var users by useState(emptyList<User>())
	useAsyncEffectOnce {
		users = client.listUsers()
	}

	Card {
		title = "Alertes de sécurité"
		subtitle = if (recentOnly) "Pendant les 7 derniers jours" else "Toutes les alertes"

		if (alerts == null)
			LoadingSpinner()
		else {
			for (alert in selectedAlerts) {
				Alert {
					this.alert = alert
					this.users = users
				}
			}

			if (selectedAlerts.isEmpty())
				+"Aucune alerte sur cette période"
		}

		action(if (recentOnly) "Afficher toutes les alertes" else "Afficher uniquement les alertes récentes") {
			recentOnly = !recentOnly
		}
		action("Actualiser") { refresh() }
	}
}

private external interface AlertProps : Props {
	var alert: Alert

	var users: List<User>
}

private val Alert = FC<AlertProps>("Alert") { props ->
	val (client) = useClient()
	require(client is Client.Authenticated) { "Les utilisateurs non-connectés ne peuvent pas afficher les alertes" }

	var forceRenderCounter by useState(0)

	val background = when (props.alert.level) {
		Alert.Level.Low -> ""
		Alert.Level.Medium -> "bg-orange-200"
		Alert.Level.High -> "bg-red-200"
	}

	val user = props.alert.user?.apply {
		loadIfNecessary(props.users, allowNotFound = true)
	}

	val timestamp = Date(props.alert.timestamp)

	p {
		classes = "rounded-lg p-2 my-1 $background"

		+props.alert.message

		if (user?.loaded == true) {
			br()
			+"Utilisateur responsable : ${user.obj.fullName} (${user.obj.email.email})"
			if (user.obj.administrator)
				+" (administrateur)"
			if (!user.obj.enabled)
				+" (actuellement désactivé)"

			if (user.id != client.me.email.email && props.users.isNotEmpty() && user.obj.enabled)
				StyledButton {
					text = "Bloquer cet utilisateur"
					action = {
						client.editUser(user.obj, enabled = false)
						forceRenderCounter++
					}
				}
		} else {
			br()
			+"Utilisateur responsable : "
			LoadingSpinner()
		}

		br()
		+"Date : ${timestamp.toLocaleString()}"
	}
}
