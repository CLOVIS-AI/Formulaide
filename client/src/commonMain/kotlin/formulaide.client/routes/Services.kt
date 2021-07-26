package formulaide.client.routes

import formulaide.api.types.Ref.Companion.createRef
import formulaide.api.users.Service
import formulaide.api.users.ServiceModification
import formulaide.client.Client

/**
 * Finds the list of all services.
 *
 * - GET /services/list
 * - Requires 'employee' authentication
 * - Response: list of [services][Service]
 */
suspend fun Client.Authenticated.listServices(): List<Service> =
	get("/services/list")

/**
 * Finds the list of all services, including those are that closed.
 *
 * - GET /services/fullList
 * - Requires 'administrator' authentication
 * - Response: list of [services][Service]
 */
suspend fun Client.Authenticated.listAllServices(): List<Service> =
	get("/services/fullList")

/**
 * Creates a new service with the given [name].
 *
 * - POST /services/create
 * - Requires 'administrator' authentication
 * - Response: [Service]
 */
suspend fun Client.Authenticated.createService(name: String): Service =
	post("/services/create", body = name)

/**
 * Closes a [service].
 *
 * - POST /services/close
 * - Requires 'administrator' authentication
 * - Body: [ServiceModification]
 * - Response: [Service]
 */
suspend fun Client.Authenticated.closeService(service: Service): Service =
	post("/services/close", body = ServiceModification(service.createRef(), false))

/**
 * Opens a [service].
 *
 * - POST /services/close
 * - Requires 'administrator' authentication
 * - Body: [ServiceModification]
 * - Response: [Service]
 */
suspend fun Client.Authenticated.reopenService(service: Service): Service =
	post("/services/close", body = ServiceModification(service.createRef(), true))
