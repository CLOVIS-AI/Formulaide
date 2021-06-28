package formulaide.client.routes

import formulaide.api.data.Composite
import formulaide.client.Client

/**
 * Finds the list of all available data.
 *
 * - GET /data/list
 * - Requires 'employee' authentication
 * - Response: list of [Composite]
 */
suspend fun Client.Authenticated.listData() =
	get<List<Composite>>("/data/list")

/**
 * Creates a new [Composite].
 *
 * - POST /data/create
 * - Requires 'administrator' authentication
 * - Body: [Composite]
 * - Response: [Composite]
 */
suspend fun Client.Authenticated.createData(newData: Composite) =
	post<Composite>("/data/create", body = newData)
