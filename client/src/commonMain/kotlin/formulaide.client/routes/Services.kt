package formulaide.client.routes

import formulaide.api.users.Service
import formulaide.client.Client

/**
 * Finds the list of all services.
 *
 * - GET /services/list
 * - Requires 'employee' authentication
 * - Response: list of [services][Service]
 */
suspend fun Client.Authenticated.listServices() =
	get<List<Service>>("/services/list")

/**
 * Finds the list of all services, including those are that closed.
 *
 * - GET /services/fullList
 * - Requires 'administrator' authentication
 * - Response: list of [services][Service]
 */
suspend fun Client.Authenticated.listAllServices() =
	get<List<Service>>("/services/fullList")

/**
 * Creates a new service with the given [name].
 *
 * - POST /services/create
 * - Requires 'administrator' authentication
 * - Response: [Service]
 */
suspend fun Client.Authenticated.createService(name: String) =
	post<Service>("/services/create", body = name)
