package formulaide.client.routes

import formulaide.api.data.Config
import formulaide.client.Client

/**
 * Gets the current configuration.
 *
 * - GET /config
 * - Response: [Config]
 */
suspend fun Client.getConfig(): Config =
	get("/config")
