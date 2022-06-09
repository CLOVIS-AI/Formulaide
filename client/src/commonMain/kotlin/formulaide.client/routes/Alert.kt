package formulaide.client.routes

import formulaide.api.data.Alert
import formulaide.client.Client

/**
 * Gets the full list of alerts.
 *
 * - GET /alerts
 * - Requires 'administrator' authentication
 * - Response: list of [Alert]
 */
suspend fun Client.Authenticated.alerts(): List<Alert> =
	get("/alerts")
